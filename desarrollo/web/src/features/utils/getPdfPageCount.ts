export async function getPdfPageCount(
  file: File
): Promise<number> {

  const pdfjsLib = await import(
    "pdfjs-dist"
  );

  pdfjsLib.GlobalWorkerOptions.workerSrc =
    new URL(
      "pdfjs-dist/build/pdf.worker.mjs",
      import.meta.url
    ).toString();

  const arrayBuffer =
    await file.arrayBuffer();

  const pdf =
    await pdfjsLib.getDocument({
      data: arrayBuffer,
    }).promise;

  return pdf.numPages;
}