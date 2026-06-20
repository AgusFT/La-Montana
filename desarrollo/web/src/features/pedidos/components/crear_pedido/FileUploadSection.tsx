import { useRef } from "react";

interface FileUploadSectionProps {
  file: File | null;
  onFileSelect: (file: File) => void;
}


export function FileUploadSection({
  file,
  onFileSelect,
}: FileUploadSectionProps) {

  const inputRef = useRef<HTMLInputElement>(null);

  function openFilePicker() {
  inputRef.current?.click();
}

// funcion que valida el tamaño del archivo
  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const selectedFile = event.target.files?.[0];
      if (!selectedFile) return;
          const maxSize = 10 * 1024 * 1024;
      if (selectedFile.size > maxSize) {
        alert(
          "El archivo supera los 10 MB permitidos."
        );
        return;
      }
    onFileSelect(selectedFile);
  }

  return (
    <section className="file-upload-section panel-card">
      <div className="section-title">
        <span className="step-badge">1</span>
        <h2>Cargá tu archivo</h2>
      </div>

    <div className="upload-layout">
      <div className="upload-dropzone">
        <div className="upload-content">
          <div className="upload-icon"><h1>☁</h1></div>

          <p className="upload-title">
            Arrastrá y soltá tu archivo aquí
          </p>

          <p className="upload-subtitle">
            o seleccioná desde tu dispositivo
          </p>

            <button
              type="button"
              className="primary-button"
              onClick={openFilePicker}
            >
              Seleccionar archivo
            </button>

            {file && (
              <div className="selected-file">
              <strong>{file.name}</strong>

              <p>
                 {(file.size / 1024 / 1024).toFixed(2)} MB
              </p>
             </div>
            )}

          <input
            ref={inputRef}
            type="file"
            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
            onChange={handleChange}
            hidden
          />

          <p className="upload-info">
            Formatos permitidos: PDF, DOC, DOCX, JPG, PNG
          </p>

          <p className="upload-info">
            Tamaño máximo: 10 MB por archivo
          </p>
        </div>
      </div>
      <aside className="upload-info-card">
        <h3 className="upload-title">
          Al cargar el archivo
        </h3>

        <ul className="upload-benefits">
          <li className="upload-benefit">
            <span className="upload-benefit-icon">
              📋
            </span>

            <span>
              Detectaremos automáticamente
              el número de páginas.
            </span>
          </li>

          <li className="upload-benefit">
            <span className="upload-benefit-icon">
              ✔️
            </span>

            <span>
              Máximo 10 MB por archivo.
            </span>
          </li>
        </ul>
      </aside>
    </div>

</section>
  );
}