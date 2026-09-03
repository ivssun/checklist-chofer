import logoPanissimo from '../assets/panissimo-logo.png'

function fmtFecha(timestamp) {
  if (!timestamp) return ''
  return timestamp.toDate().toLocaleDateString('es-MX', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function fmtFechaHora(timestamp) {
  if (!timestamp) return ''
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

function esValorNa(valor) {
  return String(valor).toUpperCase() === 'N/A'
}

function Casilla({ marcada }) {
  return <span className="cli-check">{marcada ? '☒' : '☐'}</span>
}

function Encabezado({ viaje }) {
  return (
    <div className="cli-encabezado">
      <img src={logoPanissimo} alt="Panissimo" className="cli-logo" />
      <div className="cli-titulo">CHECK LIST DIARIO DE CHOFER</div>
      <div className="cli-folio">{viaje.id}</div>
    </div>
  )
}

function Pie() {
  return (
    <div className="cli-pie">
      <span>Firma del colaborador: ______________________________</span>
      <span>Firma de checador / supervisor: ______________________________</span>
    </div>
  )
}

function FilaSiNo({ label, campo }) {
  const valor = campo?.valor
  return (
    <tr>
      <td className="cli-td-label">{label}</td>
      <td className="cli-td-check"><Casilla marcada={esValorPositivo(valor)} /></td>
      <td className="cli-td-check"><Casilla marcada={esValorNegativo(valor)} /></td>
      <td className="cli-td-obs">{campo?.observacion || ''}</td>
    </tr>
  )
}

function FilaInspeccion({ label, campo }) {
  const valor = campo?.valor
  return (
    <tr>
      <td className="cli-td-label">{label}</td>
      <td className="cli-td-check"><Casilla marcada={esValorPositivo(valor)} /></td>
      <td className="cli-td-check"><Casilla marcada={esValorNegativo(valor)} /></td>
      <td className="cli-td-check"><Casilla marcada={esValorNa(valor)} /></td>
      <td className="cli-td-obs">{campo?.observacion || ''}</td>
    </tr>
  )
}

function FilaLibre({ label, valor, observacion }) {
  return (
    <tr>
      <td className="cli-td-label">{label}</td>
      <td className="cli-td-obs" colSpan={3}>{valor}</td>
      <td className="cli-td-obs">{observacion || ''}</td>
    </tr>
  )
}

export default function ImpresionChecklist({ viaje, chofer, destinos, cargasCombustible, destinosMap }) {
  const tipoMarcado = (tipo) => viaje.tipoUnidad === tipo

  return (
    <div className="cli-imprimible">
      {/* Página 1 */}
      <div className="cli-hoja">
        <Encabezado viaje={viaje} />

        <table className="cli-tabla-datos">
          <tbody>
            <tr>
              <td className="cli-th-label">NOMBRE DEL COLABORADOR</td>
              <td className="cli-td-valor">{chofer?.nombre || ''}</td>
              <td className="cli-th-label">FECHA</td>
              <td className="cli-td-valor">{fmtFecha(viaje.fecha)}</td>
              <td className="cli-th-label">No. DE UNIDAD / ECONÓMICO</td>
              <td className="cli-td-valor">{viaje.economico}</td>
            </tr>
            <tr>
              <td className="cli-th-label">TIPO DE UNIDAD</td>
              <td className="cli-td-valor">
                <Casilla marcada={tipoMarcado('GDE')} /> ISUZU GDE{'  '}
                <Casilla marcada={tipoMarcado('MED')} /> ISUZU MED{'  '}
                <Casilla marcada={tipoMarcado('RENTA')} /> ISUZU RENTA
              </td>
              <td className="cli-th-label">PLACAS</td>
              <td className="cli-td-valor">{viaje.placa}</td>
              <td className="cli-th-label">{viaje.detalleRenta ? 'DETALLE' : ''}</td>
              <td className="cli-td-valor">{viaje.detalleRenta || ''}</td>
            </tr>
          </tbody>
        </table>

        <div className="cli-seccion">BITÁCORA DE RUTA POR DESTINO</div>
        <table className="cli-tabla-datos">
          <tbody>
            <tr>
              <td className="cli-hora-label">HORA DE LLEGADA A LA MATRIZ</td>
              <td className="cli-hora-valor">{fmtFechaHora(viaje.horaLlegadaMatriz)}</td>
            </tr>
          </tbody>
        </table>
        <table className="cli-tabla">
          <thead>
            <tr>
              <th>DESTINO</th>
              <th>CEDIS DESTINO</th>
              <th>KM INICIAL</th>
              <th>KM FINAL</th>
              <th>FECHA SALIDA</th>
              <th>FECHA LLEGADA</th>
              <th>CANASTILLAS ENTREGADAS</th>
              <th>CANASTILLAS QUE REGRESÓ</th>
            </tr>
          </thead>
          <tbody>
            {destinos.map((d) => (
              <tr key={d.id}>
                <td>{d.orden}</td>
                <td>{destinosMap[d.cedisDestino] || d.cedisDestino}</td>
                <td>{d.kmInicial ?? ''}</td>
                <td>{d.kmFinal ?? ''}</td>
                <td>{fmtFechaHora(d.fechaSalida)}</td>
                <td>{fmtFechaHora(d.fechaLlegada)}</td>
                <td>{d.canastillasEntregadas ?? ''}</td>
                <td>{d.canastillasRegresadas ?? ''}</td>
              </tr>
            ))}
            {destinos.length === 0 && (
              <tr><td colSpan={8}>Sin destinos registrados</td></tr>
            )}
          </tbody>
        </table>

        <div className="cli-seccion">COMBUSTIBLE Y LIMPIEZA</div>
        <table className="cli-tabla">
          <thead>
            <tr>
              <th className="cli-th-concepto">CONCEPTO</th>
              <th>SÍ</th>
              <th>NO</th>
              <th className="cli-th-obs">OBSERVACIONES</th>
            </tr>
          </thead>
          <tbody>
            <FilaSiNo label="¿La unidad salió con el tanque lleno de diésel?" campo={viaje.combustibleYLimpieza?.tanqueLlenoSalida} />
            <FilaSiNo label="¿Se dejó la unidad con el tanque lleno al regresar a la planta?" campo={viaje.combustibleYLimpieza?.tanqueLlenoRegreso} />
            <FilaSiNo label="¿Se realizó limpieza de caja y cabina?" campo={viaje.combustibleYLimpieza?.limpiezaCajaCabina} />
          </tbody>
        </table>

        <div className="cli-seccion">CARGA DE COMBUSTIBLE</div>
        <table className="cli-tabla">
          <thead>
            <tr>
              <th>NÚMERO DE CARGA</th>
              <th>UBICACIÓN</th>
              <th>KILOMETRAJE AL CARGAR</th>
              <th>COSTO POR LITRO</th>
              <th>LITROS CARGADOS</th>
              <th>FECHA DE CARGA</th>
            </tr>
          </thead>
          <tbody>
            {cargasCombustible.map((c, i) => (
              <tr key={c.id}>
                <td>{i + 1}</td>
                <td>{c.ubicacion}</td>
                <td>{c.kilometraje}</td>
                <td>{c.costoPorLitro}</td>
                <td>{c.litros}</td>
                <td>{fmtFechaHora(c.fechaCarga)}</td>
              </tr>
            ))}
            {cargasCombustible.length === 0 && (
              <tr><td colSpan={6}>Sin cargas registradas</td></tr>
            )}
          </tbody>
        </table>

        <Pie />
      </div>

      {/* Página 2 */}
      <div className="cli-hoja">
        <Encabezado viaje={viaje} />

        <div className="cli-seccion">INSPECCIÓN GENERAL DE LA UNIDAD (ISUZU)</div>
        <table className="cli-tabla">
          <thead>
            <tr>
              <th className="cli-th-concepto">PUNTO A REVISAR</th>
              <th>BIEN</th>
              <th>MAL</th>
              <th>N/A</th>
              <th className="cli-th-obs">OBSERVACIONES</th>
            </tr>
          </thead>
          <tbody>
            <FilaInspeccion label="Llantas (desgaste)" campo={viaje.inspeccionGeneral?.llantasDesgaste} />
            <FilaInspeccion label="Llanta de refacción" campo={viaje.inspeccionGeneral?.llantaRefaccion} />
            <FilaLibre
              label="Presión de llantas"
              valor={(viaje.presionLlantas || []).map((p) => `${p.etiqueta}: ${p.presion} psi`).join(', ')}
              observacion={viaje.presionLlantasObservacion?.observacion}
            />
            <FilaInspeccion label="Sistema de frenado óptimo" campo={viaje.inspeccionGeneral?.sistemaFrenado} />
            <FilaInspeccion label="Luces (altas, bajas, direccionales, reversa, stop)" campo={viaje.inspeccionGeneral?.luces} />
            <FilaInspeccion label="Espejos laterales y retrovisor" campo={viaje.inspeccionGeneral?.espejos} />
            <FilaInspeccion label="Limpiaparabrisas y claxon" campo={viaje.inspeccionGeneral?.limpiaparabrisas} />
            <FilaInspeccion label="Nivel de aceite de motor" campo={viaje.inspeccionGeneral?.nivelAceite} />
            <FilaInspeccion label="Nivel de agua / refrigerante" campo={viaje.inspeccionGeneral?.nivelAgua} />
            <FilaInspeccion label="Nivel de líquido de frenos (en caso de que aplique)" campo={viaje.inspeccionGeneral?.nivelLiquidoFreno} />
            <FilaLibre label="Nivel de Urea" valor={`${viaje.ureaPorcentaje ?? ''}%`} observacion={viaje.ureaObservacion?.observacion} />
            <FilaLibre label="Nivel de combustible para thermo" valor={viaje.combustibleThermo ?? ''} observacion={viaje.combustibleThermoObservacion?.observacion} />
            <FilaInspeccion label="Batería (terminales y carga)" campo={viaje.inspeccionGeneral?.bateria} />
            <FilaInspeccion label="Triángulos de seguridad / señalización" campo={viaje.inspeccionGeneral?.triangulos} />
            <FilaInspeccion label="Gato hidráulico, cruceta y herramienta básica" campo={viaje.inspeccionGeneral?.gato} />
            <FilaInspeccion label="Estado de Carrocería (golpes, rayones, abolladuras)" campo={viaje.inspeccionGeneral?.carroceria} />
            <FilaInspeccion label="Candados de caja" campo={viaje.inspeccionGeneral?.candados} />
            <FilaInspeccion label="Bandas de seguridad para la caja" campo={viaje.inspeccionGeneral?.bandas} />
          </tbody>
        </table>

        <Pie />
      </div>

      {/* Página 3 */}
      <div className="cli-hoja">
        <Encabezado viaje={viaje} />

        <div className="cli-seccion">DOCUMENTACIÓN Y EQUIPO DE LA UNIDAD</div>
        <table className="cli-tabla">
          <thead>
            <tr>
              <th className="cli-th-concepto">DOCUMENTO / EQUIPO</th>
              <th>SÍ</th>
              <th>NO</th>
              <th className="cli-th-obs">OBSERVACIONES</th>
            </tr>
          </thead>
          <tbody>
            <FilaSiNo label="Copia de SÚA (Seguro Social)" campo={viaje.documentacionEquipo?.licenciaChofer} />
            <FilaSiNo label="Copia de póliza de seguro vigente" campo={viaje.documentacionEquipo?.tarjetaCirculacion} />
            <FilaSiNo label="Tarjeta de circulación" campo={viaje.documentacionEquipo?.segurosVehiculo} />
            <FilaSiNo label="Equipo de seguridad completo (chaleco, botas, casco, guantes, lentes)" campo={viaje.documentacionEquipo?.documentoViaje} />
          </tbody>
        </table>

        <div className="cli-obs-generales">
          <b>Observaciones generales:</b>
          <div className="cli-obs-caja">{viaje.observacionesGenerales || ''}</div>
        </div>

        <Pie />
      </div>
    </div>
  )
}
