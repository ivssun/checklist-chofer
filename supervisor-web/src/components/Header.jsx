import { Group, Image, Text } from '@mantine/core'
import logo from '../assets/panissimo-logo.png'

export default function Header() {
  return (
    <Group
      justify="space-between"
      px="md"
      py="xs"
      style={{ borderBottom: '3px solid var(--mantine-color-panissimo-7)' }}
    >
      <Image src={logo} h={32} w="auto" fit="contain" />
      <Text size="sm" c="dimmed" fw={500}>
        Panel de Supervisor
      </Text>
    </Group>
  )
}
