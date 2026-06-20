export const MAX_UPLOAD_FILE_COUNT = 30
export const MAX_UPLOAD_TOTAL_SIZE_MB = 500
const BYTES_PER_MEGABYTE = 1024 * 1024
const MAX_UPLOAD_TOTAL_SIZE_BYTES = MAX_UPLOAD_TOTAL_SIZE_MB * BYTES_PER_MEGABYTE

/**
 * 校验上传批次文件数量和总体积。
 *
 * @param files - 待上传文件
 * @returns 校验消息，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
export function validateUploadBatchFiles(files: File[]): string {
  if (files.length > MAX_UPLOAD_FILE_COUNT) {
    // 文件数量超出产品上限时阻止请求发出。
    return `单个批次最多上传 ${MAX_UPLOAD_FILE_COUNT} 个文件，请拆分后再上传`
  } else if (totalUploadSize(files) > MAX_UPLOAD_TOTAL_SIZE_BYTES) {
    // 总大小超出服务端上限时阻止大请求进入网络。
    return `单个批次最多上传 ${MAX_UPLOAD_TOTAL_SIZE_MB} MB，请拆分后再上传`
  } else {
    // 文件数量和总体积都满足限制。
    return ''
  }
}

/**
 * 计算上传批次总字节数。
 *
 * @param files - 待上传文件
 * @returns 文件总字节数
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function totalUploadSize(files: File[]): number {
  return files.reduce((sum, file) => sum + file.size, 0)
}
