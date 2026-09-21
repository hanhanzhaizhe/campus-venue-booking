import { ElMessage } from 'element-plus'

/** Show API / network error message via Element Plus */
export function showError(err: unknown, fallback = '请求失败'): void {
  const msg =
    typeof err === 'string'
      ? err
      : err && typeof err === 'object' && 'message' in err
        ? String((err as { message: unknown }).message)
        : fallback
  ElMessage.error(msg || fallback)
}

/**
 * Build purpose field for create/reschedule body.
 * - clearPurpose=true → purpose: ""
 * - nonempty trimmed → purpose: trimmed
 * - empty/whitespace → omit purpose key
 */
export function withPurpose<T extends object>(
  base: T,
  purposeInput: string,
  clearPurpose = false,
): T & { purpose?: string } {
  if (clearPurpose) {
    return { ...base, purpose: '' }
  }
  const trimmed = purposeInput.trim()
  if (trimmed) {
    return { ...base, purpose: trimmed }
  }
  return { ...base }
}
