/**
 * Reads an uploaded image, scales it down and re-encodes it as JPEG (data URL),
 * which the API stores as-is for now. TODO: real file upload endpoint + storage,
 * keeping only the image URL in the database.
 */
export async function imageFileToDataUrl(file: File, maxSize = 1200, quality = 0.8): Promise<string> {
  if (!file.type.startsWith('image/')) throw new Error('Le fichier doit être une image.');
  if (file.size > 15 * 1024 * 1024) throw new Error('Image trop lourde (15 Mo maximum).');

  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return canvas.toDataURL('image/jpeg', quality);
}
