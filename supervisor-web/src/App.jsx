import { Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import ViajeDetalle from './pages/ViajeDetalle'
import Catalogos from './pages/Catalogos'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/viajes/:viajeId" element={<ViajeDetalle />} />
      <Route path="/catalogos" element={<Catalogos />} />
    </Routes>
  )
}

export default App
