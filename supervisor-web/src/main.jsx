import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@mantine/core/styles.css'
import './index.css'
import { createTheme, MantineProvider } from '@mantine/core'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'

const theme = createTheme({
  primaryColor: 'panissimo',
  primaryShade: 7,
  colors: {
    panissimo: [
      '#ECF8F2',
      '#D9F2E5',
      '#BAE8D0',
      '#93DDB5',
      '#62D095',
      '#36BF76',
      '#2B975D',
      '#1F6E44',
      '#174F31',
      '#123623',
    ],
  },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <MantineProvider theme={theme}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </MantineProvider>
  </StrictMode>,
)
