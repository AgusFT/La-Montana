interface FileUploadSectionProps {
  file: File | null;
  onFileSelect: (file: File) => void;
}

export function FileUploadSection({
  file,
  onFileSelect,
}: FileUploadSectionProps) {
  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const selectedFile = event.target.files?.[0];

    if (!selectedFile) return;

    onFileSelect(selectedFile);
  }

  return (
    // <section className="panel-card">
    //   <h2>Cargar archivo</h2>

    // </section>
    
    <section className="file-upload-section panel-card">
  <div className="section-title">
    <span className="step-badge">1</span>
    <h2>Cargá tu archivo</h2>
  </div>

  <div className="upload-dropzone">
    <div className="upload-content">
      <div className="upload-icon">☁</div>

        {/* <input
          type="file"
          accept=".pdf"
          onChange={handleChange}
        />
    
        {file && (
          <div>
            <p>{file.name}</p>
            <p>{(file.size / 1024 / 1024).toFixed(2)} MB</p>
          </div>
        )} */}

      <p className="upload-title">
        Arrastrá y soltá tu archivo aquí
      </p>

      <p className="upload-subtitle">
        o seleccioná desde tu dispositivo
      </p>

      <button
        type="button"
        className="primary-button"
      >
        Seleccionar archivo
      </button>

      <p className="upload-info">
        Formatos permitidos: PDF, DOC, DOCX, JPG, PNG
      </p>

      <p className="upload-info">
        Tamaño máximo: 10 MB por archivo
      </p>
    </div>
  </div>
</section>
  );
}