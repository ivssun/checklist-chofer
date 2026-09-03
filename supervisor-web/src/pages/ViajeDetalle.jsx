import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { collection, doc, getDoc, getDocs, orderBy, query } from 'firebase/firestore'
import {
  Alert,
  Anchor,
  Badge,
  Button,
  Container,
  Group,
  Loader,
  Paper,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core'
import { db } from '../firebase'
import ImpresionChecklist from './ImpresionChecklist'
import Header from '../components/Header'

function formatFecha(timestamp) {
  if (!timestamp) return '—'
  return timestamp.toDate().toLocaleString('es-MX', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function esValorPositivo(valor) {
  if (typeof valor === 'boolean') return valor
  return ['SÍ', 'SI', 'BIEN'].includes(String(valor).toUpperCase())
}

function esValorNegativo(valor) {
  if (typeof valor === 'boolean') return !valor
  return ['NO', 'MAL'].includes(String(valor).toUpperCase())
}

function colorValor(valor) {
  if (esValorPositivo(valor)) return 'panissimo'
  if (esValorNegativo(valor)) return 'red'
  return 'gray'
}

function textoValor(valor) {
  if (typeof valor === 'boolean') return valor ? 'SÍ' : 'NO'
  return String(valor).toUpperCase()
}

const CAMPOS_COMBUSTIBLE_LIMPIEZA = [
  ['tanqueLlenoSalida', 'Tanque lleno (salida)'],
  ['tanqueLlenoRegreso', 'Tanque lleno (regreso)'],
  ['limpiezaCajaCabina', 'Limpieza de caja y cabina'],
]

const CAMPOS_INSPECCION = [
  ['llantasDesgaste', 'Llantas (desgaste)'],
  ['llantaRefaccion', 'Llanta de refacción'],
  ['sistemaFrenado', 'Sistema de frenado'],
  ['luces', 'Luces'],
  ['espejos', 'Espejos'],
  ['limpiaparabrisas', 'Limpiaparabrisas y claxon'],
  ['nivelAceite', 'Nivel de aceite de motor'],
  ['nivelAgua', 'Nivel de agua / refrigerante'],
  ['nivelLiquidoFreno', 'Nivel de líquido de frenos'],
  ['bateria', 'Batería'],
  ['triangulos', 'Triángulos de seguridad'],
  ['gato', 'Gato hidráulico, cruceta, herramienta'],
  ['carroceria', 'Carrocería'],
  ['candados', 'Candados de caja'],
  ['bandas', 'Bandas de seguridad de caja'],
]

const CAMPOS_DOCUMENTACION = [
  ['licenciaChofer', 'Copia de SÚA (Seguro Social)'],
  ['tarjetaCirculacion', 'Póliza de seguro vigente'],
  ['segurosVehiculo', 'Tarjeta de circulación'],
  ['documentoViaje', 'Equipo de seguridad completo'],
]

function SeccionTitulo({ children }) {
  return (
    <Title
      order={3}
      c="panissimo"
      fw={700}
      mt="xl"
      mb="xs"
      style={{ borderLeft: '4px solid var(--mantine-color-panissimo-7)', paddingLeft: 10 }}
    >
      {children}
    </Title>
  )
}

function EtiquetaCampo({ children }) {
  return (
    <Text size="sm" fw={700} c="panissimo" tt="uppercase">
      {children}
    </Text>
  )
}

function CampoCheck({ label, campo }) {
  if (!campo) return null
  return (
    <Paper withBorder p="sm">
      <Group justify="space-between" wrap="nowrap">
        <Text size="sm">{label}</Text>
        <Badge color={colorValor(campo.valor)}>{textoValor(campo.valor)}</Badge>
      </Group>
      {campo.observacion && (
        <Text size="xs" c="dimmed" mt={4}>
          {campo.observacion}
        </Text>
      )}
      {campo.fotoURL && (
        <Anchor size="xs" href={campo.fotoURL} target="_blank" mt={4} display="block">
          Ver foto
        </Anchor>
      )}
    </Paper>
  )
}

export default function ViajeDetalle() {
  const { viajeId } = useParams()
  const navigate = useNavigate()

  const [viaje, setViaje] = useState(undefined)
  const [chofer, setChofer] = useState(null)
  const [camion, setCamion] = useState(null)
  const [destinosCatalogo, setDestinosCatalogo] = useState([])
  const [destinos, setDestinos] = useState([])
  const [cargasCombustible, setCargasCombustible] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    async function cargar() {
      try {
        const viajeSnap = await getDoc(doc(db, 'viajes', viajeId))
        if (!viajeSnap.exists()) {
          setViaje(null)
          return
        }
        const viajeData = { id: viajeSnap.id, ...viajeSnap.data() }
        setViaje(viajeData)

        const [choferSnap, destinosCatalogoSnap, destinosSnap, cargasSnap] = await Promise.all([
          viajeData.choferId ? getDoc(doc(db, 'choferes', viajeData.choferId)) : Promise.resolve(null),
          getDocs(collection(db, 'destinosCatalogo')),
          getDocs(query(collection(db, 'viajes', viajeId, 'destinos'), orderBy('orden'))),
          getDocs(collection(db, 'viajes', viajeId, 'cargasCombustible')),
        ])

        if (choferSnap && choferSnap.exists()) {
          setChofer({ id: choferSnap.id, ...choferSnap.data() })
        }
        setDestinosCatalogo(destinosCatalogoSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setDestinos(destinosSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setCargasCombustible(cargasSnap.docs.map((d) => ({ id: d.id, ...d.data() })))

        if (viajeData.camionId) {
          const camionSnap = await getDoc(doc(db, 'camiones', viajeData.camionId))
          if (camionSnap.exists()) setCamion({ id: camionSnap.id, ...camionSnap.data() })
        }
      } catch (e) {
        setError(e.message)
      }
    }
    cargar()
  }, [viajeId])

  const destinosMap = useMemo(
    () => Object.fromEntries(destinosCatalogo.map((d) => [d.id, d.nombre])),
    [destinosCatalogo]
  )

  const rendimiento = useMemo(() => {
    if (destinos.length === 0) return null
    const primero = destinos[0]
    const ultimo = destinos[destinos.length - 1]
    if (primero.kmInicial == null || ultimo.kmFinal == null) return null

    const tanqueLlenoSalida = viaje?.combustibleYLimpieza?.tanqueLlenoSalida?.valor
    const tanqueLlenoRegreso = viaje?.combustibleYLimpieza?.tanqueLlenoRegreso?.valor
    if (!esValorPositivo(tanqueLlenoSalida) || !esValorPositivo(tanqueLlenoRegreso)) return null

    const litrosTotales = cargasCombustible.reduce((acc, c) => acc + (c.litros || 0), 0)
    if (litrosTotales <= 0) return null

    const km = ultimo.kmFinal - primero.kmInicial
    if (km <= 0) return null

    return km / litrosTotales
  }, [destinos, cargasCombustible, viaje])

  const alertaServicio = useMemo(() => {
    if (!camion || destinos.length === 0) return null
    const primero = destinos[0]
    if (primero.kmInicial == null) return null
    const diferencia = primero.kmInicial - (camion.kilometrajeUltimoServicio ?? 0)
    return diferencia >= 9000 ? diferencia : null
  }, [camion, destinos])

  function handleImprimir() {
    const tituloOriginal = document.title
    document.title = `Checklist_${viaje.id}`
    const restaurar = () => {
      document.title = tituloOriginal
      window.removeEventListener('afterprint', restaurar)
    }
    window.addEventListener('afterprint', restaurar)
    window.print()
  }

  if (error) {
    return (
      <Container size="lg" py="xl">
        <Alert color="red" title="Error cargando el viaje">
          {error}
        </Alert>
      </Container>
    )
  }

  if (viaje === undefined) {
    return (
      <Container size="lg" py="xl">
        <Loader mt="xl" />
      </Container>
    )
  }

  if (viaje === null) {
    return (
      <Container size="lg" py="xl">
        <Alert color="red" title="Viaje no encontrado">
          No existe un viaje con ese ID.
        </Alert>
        <Button mt="md" onClick={() => navigate('/')}>
          Volver al Dashboard
        </Button>
      </Container>
    )
  }

  return (
    <>
    <div className="no-imprimir">
      <Header />
    </div>
    <Container size="lg" py="xl" className="no-imprimir">
      <Group justify="space-between">
        <Button variant="subtle" onClick={() => navigate('/')}>
          ← Volver al Dashboard
        </Button>
        <Group>
          <Button onClick={handleImprimir}>Imprimir</Button>
          <Badge color={viaje.concluido ? 'panissimo' : 'blue'} size="lg">
            {viaje.concluido ? 'Concluido' : 'Activo'}
          </Badge>
        </Group>
      </Group>

      <Title order={1} mt="md">
        Viaje — {chofer?.nombre || 'Chofer'} — {viaje.placa}
      </Title>

      {alertaServicio && (
        <Alert color="orange" title="⚠️ Alerta de servicio" mt="md">
          El camión lleva {alertaServicio.toLocaleString('es-MX')} km desde su último servicio
          (umbral: 9,000 km).
        </Alert>
      )}

      <Paper withBorder p="md" mt="md">
        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <div>
            <EtiquetaCampo>Fecha</EtiquetaCampo>
            <Text>{formatFecha(viaje.fecha)}</Text>
          </div>
          <div>
            <EtiquetaCampo>Tipo de unidad</EtiquetaCampo>
            <Text>{viaje.tipoUnidad}</Text>
          </div>
          <div>
            <EtiquetaCampo>Placa</EtiquetaCampo>
            <Text>{viaje.placa}</Text>
          </div>
          <div>
            <EtiquetaCampo>Económico</EtiquetaCampo>
            <Text>{viaje.economico}</Text>
          </div>
          {viaje.detalleRenta && (
            <div>
              <EtiquetaCampo>Detalle</EtiquetaCampo>
              <Text>{viaje.detalleRenta}</Text>
            </div>
          )}
          {rendimiento && (
            <div>
              <EtiquetaCampo>Rendimiento</EtiquetaCampo>
              <Text>{rendimiento.toFixed(2)} km/L</Text>
            </div>
          )}
        </SimpleGrid>
      </Paper>

      <SeccionTitulo>Itinerario</SeccionTitulo>
      <Table mt="sm" withTableBorder striped>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>#</Table.Th>
            <Table.Th>Destino</Table.Th>
            <Table.Th>Salida</Table.Th>
            <Table.Th>Llegada</Table.Th>
            <Table.Th>Km inicial</Table.Th>
            <Table.Th>Km final</Table.Th>
            <Table.Th>Canastillas (ent./reg.)</Table.Th>
            <Table.Th>Nota</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {destinos.map((d) => (
            <Table.Tr key={d.id}>
              <Table.Td>{d.orden}</Table.Td>
              <Table.Td>{destinosMap[d.cedisDestino] || d.cedisDestino}</Table.Td>
              <Table.Td>{formatFecha(d.fechaSalida)}</Table.Td>
              <Table.Td>{formatFecha(d.fechaLlegada)}</Table.Td>
              <Table.Td>{d.kmInicial ?? '—'}</Table.Td>
              <Table.Td>{d.kmFinal ?? '—'}</Table.Td>
              <Table.Td>
                {d.canastillasEntregadas ?? '—'} / {d.canastillasRegresadas ?? '—'}
              </Table.Td>
              <Table.Td>
                {d.nota || '—'}
                {d.fotoURL && (
                  <Anchor size="xs" href={d.fotoURL} target="_blank" ml={6}>
                    (foto)
                  </Anchor>
                )}
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>

      <SeccionTitulo>Cargas de combustible</SeccionTitulo>
      <Table mt="sm" withTableBorder striped>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Fecha</Table.Th>
            <Table.Th>Ubicación</Table.Th>
            <Table.Th>Kilometraje</Table.Th>
            <Table.Th>$/Litro</Table.Th>
            <Table.Th>Litros</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {cargasCombustible.map((c) => (
            <Table.Tr key={c.id}>
              <Table.Td>{formatFecha(c.fechaCarga)}</Table.Td>
              <Table.Td>{c.ubicacion}</Table.Td>
              <Table.Td>{c.kilometraje}</Table.Td>
              <Table.Td>{c.costoPorLitro}</Table.Td>
              <Table.Td>{c.litros}</Table.Td>
            </Table.Tr>
          ))}
          {cargasCombustible.length === 0 && (
            <Table.Tr>
              <Table.Td colSpan={5}>
                <Text c="dimmed" ta="center" py="sm">Sin cargas registradas.</Text>
              </Table.Td>
            </Table.Tr>
          )}
        </Table.Tbody>
      </Table>

      <SeccionTitulo>Combustible y limpieza</SeccionTitulo>
      <SimpleGrid cols={{ base: 1, sm: 3 }} mt="sm">
        {CAMPOS_COMBUSTIBLE_LIMPIEZA.map(([key, label]) => (
          <CampoCheck key={key} label={label} campo={viaje.combustibleYLimpieza?.[key]} />
        ))}
      </SimpleGrid>

      <SeccionTitulo>Inspección general</SeccionTitulo>
      <SimpleGrid cols={{ base: 1, sm: 3 }} mt="sm">
        {CAMPOS_INSPECCION.map(([key, label]) => (
          <CampoCheck key={key} label={label} campo={viaje.inspeccionGeneral?.[key]} />
        ))}
      </SimpleGrid>

      {viaje.presionLlantas?.length > 0 && (
        <Paper withBorder p="sm" mt="sm">
          <Text size="sm" fw={500} mb={4}>Presión de llantas</Text>
          <Group gap="md">
            {viaje.presionLlantas.map((p, i) => (
              <Text size="sm" key={i}>{p.etiqueta}: {p.presion} psi</Text>
            ))}
          </Group>
          {viaje.presionLlantasObservacion?.observacion && (
            <Text size="xs" c="dimmed" mt={4}>{viaje.presionLlantasObservacion.observacion}</Text>
          )}
          {viaje.presionLlantasObservacion?.fotoURL && (
            <Anchor size="xs" href={viaje.presionLlantasObservacion.fotoURL} target="_blank" mt={4} display="block">
              Ver foto
            </Anchor>
          )}
        </Paper>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2 }} mt="sm">
        <Paper withBorder p="sm">
          <Text size="sm" fw={500}>Nivel de urea: {viaje.ureaPorcentaje ?? '—'}%</Text>
          {viaje.ureaObservacion?.observacion && (
            <Text size="xs" c="dimmed" mt={4}>{viaje.ureaObservacion.observacion}</Text>
          )}
          {viaje.ureaObservacion?.fotoURL && (
            <Anchor size="xs" href={viaje.ureaObservacion.fotoURL} target="_blank" mt={4} display="block">
              Ver foto
            </Anchor>
          )}
        </Paper>
        <Paper withBorder p="sm">
          <Text size="sm" fw={500}>Combustible Thermo: {viaje.combustibleThermo ?? '—'}</Text>
          {viaje.combustibleThermoObservacion?.observacion && (
            <Text size="xs" c="dimmed" mt={4}>{viaje.combustibleThermoObservacion.observacion}</Text>
          )}
          {viaje.combustibleThermoObservacion?.fotoURL && (
            <Anchor size="xs" href={viaje.combustibleThermoObservacion.fotoURL} target="_blank" mt={4} display="block">
              Ver foto
            </Anchor>
          )}
        </Paper>
      </SimpleGrid>

      <SeccionTitulo>Documentación y equipo</SeccionTitulo>
      <SimpleGrid cols={{ base: 1, sm: 4 }} mt="sm">
        {CAMPOS_DOCUMENTACION.map(([key, label]) => (
          <CampoCheck key={key} label={label} campo={viaje.documentacionEquipo?.[key]} />
        ))}
      </SimpleGrid>

      {viaje.observacionesGenerales && (
        <>
          <SeccionTitulo>Observaciones generales</SeccionTitulo>
          <Paper withBorder p="sm" mt="sm">
            <Text size="sm">{viaje.observacionesGenerales}</Text>
          </Paper>
        </>
      )}

      <Stack h={40} />
    </Container>

    <ImpresionChecklist
      viaje={viaje}
      chofer={chofer}
      destinos={destinos}
      cargasCombustible={cargasCombustible}
      destinosMap={destinosMap}
    />
    </>
  )
}
