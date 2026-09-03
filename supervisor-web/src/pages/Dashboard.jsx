import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { collection, doc, getDocs, orderBy, query, serverTimestamp, updateDoc, where } from 'firebase/firestore'
import {
  Alert,
  Anchor,
  Badge,
  Button,
  Container,
  Group,
  Image,
  Loader,
  Paper,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { db } from '../firebase'
import Header from '../components/Header'

function formatFecha(timestamp) {
  if (!timestamp) return '—'
  return timestamp.toDate().toLocaleDateString('es-MX', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function esValorPositivo(valor) {
  if (typeof valor === 'boolean') return valor
  return ['SÍ', 'SI', 'BIEN'].includes(String(valor).toUpperCase())
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [viajes, setViajes] = useState(null)
  const [choferes, setChoferes] = useState([])
  const [camiones, setCamiones] = useState([])
  const [destinosCatalogo, setDestinosCatalogo] = useState([])
  const [incidentesPendientes, setIncidentesPendientes] = useState([])
  const [resolviendoId, setResolviendoId] = useState(null)
  const [metricas, setMetricas] = useState({})
  const [error, setError] = useState(null)

  const [filtroChofer, setFiltroChofer] = useState(null)
  const [filtroPlaca, setFiltroPlaca] = useState(null)
  const [filtroDestino, setFiltroDestino] = useState(null)
  const [filtroFechaInicio, setFiltroFechaInicio] = useState('')
  const [filtroFechaFin, setFiltroFechaFin] = useState('')

  useEffect(() => {
    async function cargarTodo() {
      try {
        const [viajesSnap, choferesSnap, camionesSnap, destinosSnap, incidentesSnap] = await Promise.all([
          getDocs(query(collection(db, 'viajes'), orderBy('fecha', 'desc'))),
          getDocs(collection(db, 'choferes')),
          getDocs(collection(db, 'camiones')),
          getDocs(collection(db, 'destinosCatalogo')),
          getDocs(query(collection(db, 'incidentes'), where('estado', '==', 'Pendiente'))),
        ])
        setViajes(viajesSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setChoferes(choferesSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setCamiones(camionesSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setDestinosCatalogo(destinosSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        const incidentes = incidentesSnap.docs.map((d) => ({ id: d.id, ...d.data() }))
        incidentes.sort((a, b) => (b.fecha?.toMillis() ?? 0) - (a.fecha?.toMillis() ?? 0))
        setIncidentesPendientes(incidentes)
      } catch (e) {
        setError(e.message)
      }
    }
    cargarTodo()
  }, [])

  async function marcarResuelto(incidenteId) {
    setResolviendoId(incidenteId)
    try {
      await updateDoc(doc(db, 'incidentes', incidenteId), {
        estado: 'Resuelto',
        fechaResuelto: serverTimestamp(),
      })
      setIncidentesPendientes((prev) => prev.filter((i) => i.id !== incidenteId))
    } catch (e) {
      setError(e.message)
    } finally {
      setResolviendoId(null)
    }
  }

  const choferesMap = useMemo(
    () => Object.fromEntries(choferes.map((c) => [c.id, c.nombre])),
    [choferes]
  )
  const destinosMap = useMemo(
    () => Object.fromEntries(destinosCatalogo.map((d) => [d.id, d.nombre])),
    [destinosCatalogo]
  )
  const camionesMap = useMemo(
    () => Object.fromEntries(camiones.map((c) => [c.id, c])),
    [camiones]
  )

  useEffect(() => {
    if (!viajes) return
    let cancelado = false

    async function cargarMetricas() {
      const entradas = await Promise.all(
        viajes.map(async (v) => {
          const [destinosSnap, cargasSnap] = await Promise.all([
            getDocs(query(collection(db, 'viajes', v.id, 'destinos'), orderBy('orden'))),
            getDocs(collection(db, 'viajes', v.id, 'cargasCombustible')),
          ])
          const destinos = destinosSnap.docs.map((d) => d.data())
          const cargas = cargasSnap.docs.map((d) => d.data())
          const litros = cargas.reduce((acc, c) => acc + (c.litros || 0), 0)

          let rendimiento = null
          let alertaServicio = false

          if (destinos.length > 0) {
            const primero = destinos[0]
            const ultimo = destinos[destinos.length - 1]

            const tanqueSalida = v.combustibleYLimpieza?.tanqueLlenoSalida?.valor
            const tanqueRegreso = v.combustibleYLimpieza?.tanqueLlenoRegreso?.valor
            if (
              primero.kmInicial != null &&
              ultimo.kmFinal != null &&
              esValorPositivo(tanqueSalida) &&
              esValorPositivo(tanqueRegreso) &&
              litros > 0
            ) {
              const km = ultimo.kmFinal - primero.kmInicial
              if (km > 0) rendimiento = km / litros
            }

            if (v.camionId && primero.kmInicial != null) {
              const camion = camionesMap[v.camionId]
              if (camion) {
                const diferencia = primero.kmInicial - (camion.kilometrajeUltimoServicio ?? 0)
                alertaServicio = diferencia >= 9000
              }
            }
          }

          return [v.id, { litros, rendimiento, alertaServicio }]
        })
      )
      if (!cancelado) setMetricas(Object.fromEntries(entradas))
    }

    cargarMetricas()
    return () => {
      cancelado = true
    }
  }, [viajes, camionesMap])

  const placasUnicas = useMemo(() => {
    if (!viajes) return []
    return [...new Set(viajes.map((v) => v.placa).filter(Boolean))]
  }, [viajes])

  const viajesFiltrados = useMemo(() => {
    if (!viajes) return []
    return viajes.filter((v) => {
      if (filtroChofer && v.choferId !== filtroChofer) return false
      if (filtroPlaca && v.placa !== filtroPlaca) return false
      if (filtroDestino && !(v.destinosSeleccionados || []).includes(filtroDestino)) return false
      if (filtroFechaInicio && v.fecha && v.fecha.toDate() < new Date(filtroFechaInicio)) {
        return false
      }
      if (
        filtroFechaFin &&
        v.fecha &&
        v.fecha.toDate() > new Date(`${filtroFechaFin}T23:59:59`)
      ) {
        return false
      }
      return true
    })
  }, [viajes, filtroChofer, filtroPlaca, filtroDestino, filtroFechaInicio, filtroFechaFin])

  return (
    <>
    <Header />
    <Container size="xl" py="xl">
      <Group justify="space-between" align="center">
        <Title order={1}>Dashboard — Checklist de Flotilla</Title>
        <Button variant="light" color="panissimo" onClick={() => navigate('/catalogos')}>
          Administrar catálogos
        </Button>
      </Group>

      {incidentesPendientes.length > 0 && (
        <Alert color="red" title={`🚨 ${incidentesPendientes.length} problema(s) pendiente(s)`} mt="md">
          <Stack gap={6}>
            {incidentesPendientes.map((inc) => (
              <Group key={inc.id} justify="space-between" wrap="nowrap" align="flex-start">
                <Group wrap="nowrap" align="flex-start">
                  {inc.fotoURL && (
                    <Anchor href={inc.fotoURL} target="_blank">
                      <Image src={inc.fotoURL} alt="Foto del incidente" w={48} h={48} radius="sm" fit="cover" />
                    </Anchor>
                  )}
                  <div>
                    <Text size="sm">
                      {inc.choferNombre || 'Chofer'} — {inc.placa} — {inc.descripcion}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {formatFecha(inc.fecha)} ·{' '}
                      <Anchor size="xs" onClick={() => navigate(`/viajes/${inc.viajeId}`)}>
                        Ver viaje
                      </Anchor>
                    </Text>
                  </div>
                </Group>
                <Button
                  size="xs"
                  color="panissimo"
                  variant="light"
                  loading={resolviendoId === inc.id}
                  onClick={() => marcarResuelto(inc.id)}
                >
                  Marcar como resuelto
                </Button>
              </Group>
            ))}
          </Stack>
        </Alert>
      )}

      {error && (
        <Alert color="red" title="Error cargando datos" mt="md">
          {error}
        </Alert>
      )}

      <Paper withBorder p="md" mt="md">
        <Group grow>
          <Select
            label="Chofer"
            placeholder="Todos"
            data={choferes.map((c) => ({ value: c.id, label: c.nombre }))}
            value={filtroChofer}
            onChange={setFiltroChofer}
            clearable
            searchable
          />
          <Select
            label="Placa"
            placeholder="Todas"
            data={placasUnicas.map((p) => ({ value: p, label: p }))}
            value={filtroPlaca}
            onChange={setFiltroPlaca}
            clearable
            searchable
          />
          <Select
            label="Destino"
            placeholder="Todos"
            data={destinosCatalogo.map((d) => ({ value: d.id, label: d.nombre }))}
            value={filtroDestino}
            onChange={setFiltroDestino}
            clearable
            searchable
          />
          <TextInput
            label="Desde"
            type="date"
            value={filtroFechaInicio}
            onChange={(e) => setFiltroFechaInicio(e.currentTarget.value)}
          />
          <TextInput
            label="Hasta"
            type="date"
            value={filtroFechaFin}
            onChange={(e) => setFiltroFechaFin(e.currentTarget.value)}
          />
        </Group>
      </Paper>

      {viajes === null && !error && <Loader mt="xl" />}

      {viajes !== null && (
        <Table mt="md" striped highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Fecha</Table.Th>
              <Table.Th>Chofer</Table.Th>
              <Table.Th>Tipo</Table.Th>
              <Table.Th>Placa</Table.Th>
              <Table.Th>Económico</Table.Th>
              <Table.Th>Destinos</Table.Th>
              <Table.Th>Combustible cargado</Table.Th>
              <Table.Th>Rendimiento</Table.Th>
              <Table.Th>Servicio</Table.Th>
              <Table.Th>Estado</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {viajesFiltrados.map((v) => {
              const m = metricas[v.id]
              return (
                <Table.Tr
                  key={v.id}
                  onClick={() => navigate(`/viajes/${v.id}`)}
                  style={{ cursor: 'pointer' }}
                >
                  <Table.Td>{formatFecha(v.fecha)}</Table.Td>
                  <Table.Td>{choferesMap[v.choferId] || '—'}</Table.Td>
                  <Table.Td>{v.tipoUnidad}</Table.Td>
                  <Table.Td>{v.placa}</Table.Td>
                  <Table.Td>{v.economico}</Table.Td>
                  <Table.Td>
                    {(v.destinosSeleccionados || [])
                      .map((id) => destinosMap[id])
                      .filter(Boolean)
                      .join(', ') || '—'}
                  </Table.Td>
                  <Table.Td>{m ? `${m.litros.toFixed(1)} L` : '—'}</Table.Td>
                  <Table.Td>{m?.rendimiento ? `${m.rendimiento.toFixed(2)} km/L` : '—'}</Table.Td>
                  <Table.Td>
                    {m?.alertaServicio && <Badge color="orange">⚠️ Servicio</Badge>}
                  </Table.Td>
                  <Table.Td>
                    <Badge color={v.concluido ? 'panissimo' : 'blue'}>
                      {v.concluido ? 'Concluido' : 'Activo'}
                    </Badge>
                  </Table.Td>
                </Table.Tr>
              )
            })}
            {viajesFiltrados.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={10}>
                  <Text c="dimmed" ta="center" py="md">
                    No hay viajes que coincidan con los filtros.
                  </Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      )}
    </Container>
    </>
  )
}
