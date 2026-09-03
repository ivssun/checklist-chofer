import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { addDoc, collection, doc, getDocs, updateDoc } from 'firebase/firestore'
import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Container,
  Group,
  Loader,
  Modal,
  NumberInput,
  Select,
  Stack,
  Table,
  Tabs,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { db } from '../firebase'
import Header from '../components/Header'

const TIPOS_CAMION = [
  { value: 'GDE', label: 'ISUZU GDE' },
  { value: 'MED', label: 'ISUZU MED' },
  { value: 'RENTA', label: 'ISUZU RENTA' },
]

function useCatalogo(collectionName, sortField) {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)

  async function recargar() {
    try {
      const snap = await getDocs(collection(db, collectionName))
      const lista = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
      lista.sort((a, b) => String(a[sortField] || '').localeCompare(String(b[sortField] || '')))
      setItems(lista)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    recargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [collectionName])

  return { items, error, recargar }
}

function CatalogoSimple({ collectionName, etiqueta }) {
  const { items, error, recargar } = useCatalogo(collectionName, 'nombre')
  const [modalAbierto, setModalAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [nombre, setNombre] = useState('')
  const [guardando, setGuardando] = useState(false)

  function abrirNuevo() {
    setEditando(null)
    setNombre('')
    setModalAbierto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setNombre(item.nombre)
    setModalAbierto(true)
  }

  async function guardar() {
    if (!nombre.trim()) return
    setGuardando(true)
    try {
      if (editando) {
        await updateDoc(doc(db, collectionName, editando.id), { nombre: nombre.trim() })
      } else {
        await addDoc(collection(db, collectionName), { nombre: nombre.trim(), activo: true })
      }
      setModalAbierto(false)
      await recargar()
    } finally {
      setGuardando(false)
    }
  }

  async function toggleActivo(item) {
    await updateDoc(doc(db, collectionName, item.id), { activo: !item.activo })
    await recargar()
  }

  return (
    <Stack mt="md">
      <Group justify="space-between">
        <Text size="sm" c="dimmed">{items?.length ?? 0} registrados</Text>
        <Button color="panissimo" onClick={abrirNuevo}>+ Agregar {etiqueta.toLowerCase()}</Button>
      </Group>

      {error && <Alert color="red">{error}</Alert>}
      {items === null && !error && <Loader />}

      {items !== null && (
        <Table striped highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{etiqueta}</Table.Th>
              <Table.Th>Estado</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((item) => (
              <Table.Tr key={item.id}>
                <Table.Td>{item.nombre}</Table.Td>
                <Table.Td>
                  <Badge color={item.activo ? 'panissimo' : 'gray'}>
                    {item.activo ? 'Activo' : 'Inactivo'}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="flex-end">
                    <Button size="xs" variant="light" color="panissimo" onClick={() => abrirEditar(item)}>
                      Editar
                    </Button>
                    <Button
                      size="xs"
                      variant="light"
                      color={item.activo ? 'red' : 'panissimo'}
                      onClick={() => toggleActivo(item)}
                    >
                      {item.activo ? 'Eliminar' : 'Reactivar'}
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
            {items.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={3}>
                  <Text c="dimmed" ta="center" py="sm">Sin registros.</Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      )}

      <Modal
        opened={modalAbierto}
        onClose={() => setModalAbierto(false)}
        title={editando ? `Editar ${etiqueta.toLowerCase()}` : `Agregar ${etiqueta.toLowerCase()}`}
      >
        <TextInput
          label={etiqueta}
          value={nombre}
          onChange={(e) => setNombre(e.currentTarget.value)}
          data-autofocus
        />
        <Button color="panissimo" fullWidth mt="md" loading={guardando} onClick={guardar}>
          Guardar
        </Button>
      </Modal>
    </Stack>
  )
}

function CatalogoCamiones() {
  const { items, error, recargar } = useCatalogo('camiones', 'placa')
  const [modalAbierto, setModalAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [placa, setPlaca] = useState('')
  const [economico, setEconomico] = useState('')
  const [tipo, setTipo] = useState(null)
  const [km, setKm] = useState(0)
  const [posiciones, setPosiciones] = useState([''])
  const [guardando, setGuardando] = useState(false)

  function abrirNuevo() {
    setEditando(null)
    setPlaca('')
    setEconomico('')
    setTipo(null)
    setKm(0)
    setPosiciones(['Delantera Izquierda', 'Delantera Derecha', 'Trasera Izquierda', 'Trasera Derecha'])
    setModalAbierto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setPlaca(item.placa || '')
    setEconomico(item.economico || '')
    setTipo(item.tipo || null)
    setKm(item.kilometrajeUltimoServicio || 0)
    setPosiciones(item.posicionesLlantas?.length ? item.posicionesLlantas : [''])
    setModalAbierto(true)
  }

  function actualizarPosicion(i, valor) {
    setPosiciones((prev) => prev.map((p, idx) => (idx === i ? valor : p)))
  }

  function agregarPosicion() {
    setPosiciones((prev) => [...prev, ''])
  }

  function quitarPosicion(i) {
    setPosiciones((prev) => prev.filter((_, idx) => idx !== i))
  }

  const valido = placa.trim() && economico.trim() && tipo && posiciones.some((p) => p.trim())

  async function guardar() {
    if (!valido) return
    setGuardando(true)
    try {
      const datos = {
        placa: placa.trim(),
        economico: economico.trim(),
        tipo,
        kilometrajeUltimoServicio: km || 0,
        posicionesLlantas: posiciones.map((p) => p.trim()).filter(Boolean),
      }
      if (editando) {
        await updateDoc(doc(db, 'camiones', editando.id), datos)
      } else {
        await addDoc(collection(db, 'camiones'), { ...datos, activo: true })
      }
      setModalAbierto(false)
      await recargar()
    } finally {
      setGuardando(false)
    }
  }

  async function toggleActivo(item) {
    await updateDoc(doc(db, 'camiones', item.id), { activo: !item.activo })
    await recargar()
  }

  return (
    <Stack mt="md">
      <Group justify="space-between">
        <Text size="sm" c="dimmed">{items?.length ?? 0} registrados</Text>
        <Button color="panissimo" onClick={abrirNuevo}>+ Agregar camión</Button>
      </Group>

      {error && <Alert color="red">{error}</Alert>}
      {items === null && !error && <Loader />}

      {items !== null && (
        <Table striped highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Tipo</Table.Th>
              <Table.Th>Placa</Table.Th>
              <Table.Th>No. Unidad</Table.Th>
              <Table.Th>Km último servicio</Table.Th>
              <Table.Th>Llantas</Table.Th>
              <Table.Th>Estado</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((item) => (
              <Table.Tr key={item.id}>
                <Table.Td>{item.tipo}</Table.Td>
                <Table.Td>{item.placa}</Table.Td>
                <Table.Td>{item.economico}</Table.Td>
                <Table.Td>{(item.kilometrajeUltimoServicio || 0).toLocaleString('es-MX')}</Table.Td>
                <Table.Td>{item.posicionesLlantas?.length ?? 0}</Table.Td>
                <Table.Td>
                  <Badge color={item.activo ? 'panissimo' : 'gray'}>
                    {item.activo ? 'Activo' : 'Inactivo'}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="flex-end" wrap="nowrap">
                    <Button size="xs" variant="light" color="panissimo" onClick={() => abrirEditar(item)}>
                      Editar
                    </Button>
                    <Button
                      size="xs"
                      variant="light"
                      color={item.activo ? 'red' : 'panissimo'}
                      onClick={() => toggleActivo(item)}
                    >
                      {item.activo ? 'Eliminar' : 'Reactivar'}
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
            {items.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={7}>
                  <Text c="dimmed" ta="center" py="sm">Sin registros.</Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      )}

      <Modal
        opened={modalAbierto}
        onClose={() => setModalAbierto(false)}
        title={editando ? 'Editar camión' : 'Agregar camión'}
        size="md"
      >
        <Stack gap="sm">
          <Select label="Tipo" data={TIPOS_CAMION} value={tipo} onChange={setTipo} data-autofocus />
          <TextInput label="Placa" value={placa} onChange={(e) => setPlaca(e.currentTarget.value)} />
          <TextInput label="No. de Unidad" value={economico} onChange={(e) => setEconomico(e.currentTarget.value)} />
          <NumberInput label="Kilometraje del último servicio" value={km} onChange={setKm} min={0} />

          <Text size="sm" fw={500} mt="xs">Posiciones de llantas</Text>
          <Stack gap={6}>
            {posiciones.map((p, i) => (
              <Group key={i} gap="xs" wrap="nowrap">
                <TextInput
                  style={{ flex: 1 }}
                  placeholder={`Posición ${i + 1}`}
                  value={p}
                  onChange={(e) => actualizarPosicion(i, e.currentTarget.value)}
                />
                <ActionIcon
                  color="red"
                  variant="light"
                  onClick={() => quitarPosicion(i)}
                  disabled={posiciones.length <= 1}
                >
                  ✕
                </ActionIcon>
              </Group>
            ))}
            <Button variant="light" color="panissimo" size="xs" onClick={agregarPosicion}>
              + Agregar posición
            </Button>
          </Stack>

          <Button color="panissimo" fullWidth mt="md" loading={guardando} disabled={!valido} onClick={guardar}>
            Guardar
          </Button>
        </Stack>
      </Modal>
    </Stack>
  )
}

export default function Catalogos() {
  const navigate = useNavigate()

  return (
    <>
      <Header />
      <Container size="lg" py="xl">
        <Group justify="space-between">
          <Button variant="subtle" onClick={() => navigate('/')}>
            ← Volver al Dashboard
          </Button>
        </Group>

        <Title order={1} mt="md">Administración de catálogos</Title>

        <Tabs defaultValue="choferes" mt="md" color="panissimo">
          <Tabs.List>
            <Tabs.Tab value="choferes">Choferes</Tabs.Tab>
            <Tabs.Tab value="camiones">Camiones</Tabs.Tab>
            <Tabs.Tab value="destinos">Destinos</Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="choferes">
            <CatalogoSimple collectionName="choferes" etiqueta="Chofer" />
          </Tabs.Panel>
          <Tabs.Panel value="camiones">
            <CatalogoCamiones />
          </Tabs.Panel>
          <Tabs.Panel value="destinos">
            <CatalogoSimple collectionName="destinosCatalogo" etiqueta="Destino" />
          </Tabs.Panel>
        </Tabs>
      </Container>
    </>
  )
}
