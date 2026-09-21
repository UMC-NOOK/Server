import { intEnv } from "./env.js";

const CONTENT_TYPE = "image/jpeg";

let cachedImageBytes = null;
let cachedImageSize = null;

function buildImageBytes(sizeBytes) {
  const bytes = new Uint8Array(sizeBytes);
  for (let i = 0; i < sizeBytes; i++) {
    bytes[i] = i % 256;
  }
  return bytes.buffer;
}

export function imageContentType() {
  return CONTENT_TYPE;
}

export function imageSizeBytes() {
  return intEnv("IMAGE_SIZE_BYTES", 800000);
}

export function sampleImageBytes() {
  const sizeBytes = imageSizeBytes();
  if (cachedImageBytes === null || cachedImageSize !== sizeBytes) {
    cachedImageBytes = buildImageBytes(sizeBytes);
    cachedImageSize = sizeBytes;
  }
  return cachedImageBytes;
}

export function uploadUrlRequestPayload(count) {
  return {
    files: Array.from({ length: count }, () => ({
      uploadType: "record",
      contentType: CONTENT_TYPE,
    })),
  };
}
