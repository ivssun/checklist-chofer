import { useEffect, useMemo, useState } from 'react'
import { collection, getDocs, orderBy, query, where } from 'firebase/firestore'
import {
  Alert,
  Badge,
  Container,
  Group,
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

function formatFecha(timestamp) {
  if (!timestamp) return '—'
  return timestamp.toDate().toLocaleDateString('es-MX', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export default function Dashboard() {
  const [viajes, setViajes] = useState(null)
  const [choferes, setChoferes] = useState([])
  const [destinosCatalogo, setDestinosCatalogo] = useState([])
  const [incidentesPendientes, setIncidentesPendientes] = useState([])
  const [error, setError] = useState(null)

  const [filtroChofer, setFiltroChofer] = useState(null)
  const [filtroPlaca, setFiltroPlaca] = useState(null)
  const [filtroDestino, setFiltroDestino] = useState(null)
  const [filtroFechaInicio, setFiltroFechaInicio] = useState('')
  const [filtroFechaFin, setFiltroFechaFin] = useState('')

  useEffect(() => {
    async function cargarTodo() {
      try {
        const [viajesSnap, choferesSnap, destinosSnap, incidentesSnap] = await Promise.all([
          getDocs(query(collection(db, 'viajes'), orderBy('fecha', 'desc'))),
          getDocs(collection(db, 'choferes')),
          getDocs(collection(db, 'destinosCatalogo')),
          getDocs(query(collection(db, 'incidentes'), where('estado', '==', 'Pendiente'))),
        ])
        setViajes(viajesSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setChoferes(choferesSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setDestinosCatalogo(destinosSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
        setIncidentesPendientes(incidentesSnap.docs.map((d) => ({ id: d.id, ...d.data() })))
      } catch (e) {
        setError(e.message)
      }
    }
    cargarTodo()
  }, [])

  const choferesMap = useMemo(
    () => Object.fromEntries(choferes.map((c) => [c.id, c.nombre])),
    [choferes]
  )
  const destinosMap = useMemo(
    () => Object.fromEntries(destinosCatalogo.map((d) => [d.id, d.nombre])),
    [destinosCatalogo]
  )

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
    <Container size="xl" py="xl">
      <Title order={1}>Dashboard — Checklist de Flotilla</Title>

      {incidentesPendientes.length > 0 && (
        <Alert color="red" title={`🚨 ${incidentesPendientes.length} problema(s) pendiente(s)`} mt="md">
          <Stack gap={4}>
            {incidentesPendientes.map((inc) => (
              <Text key={inc.id} size="sm">
                {inc.choferNombre || 'Chofer'} — {inc.placa} — {inc.descripcion}
              </Text>
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
              <Table.Th>Estado</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {viajesFiltrados.map((v) => (
              <Table.Tr key={v.id}>
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
                <Table.Td>
                  <Badge color={v.concluido ? 'green' : 'blue'}>
                    {v.concluido ? 'Concluido' : 'Activo'}
                  </Badge>
                </Table.Td>
              </Table.Tr>
            ))}
            {viajesFiltrados.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={7}>
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
  )
}
