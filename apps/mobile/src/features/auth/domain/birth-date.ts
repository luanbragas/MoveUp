// Data de nascimento digitada como DD/MM/AAAA → ISO (AAAA-MM-DD), que é o que a API recebe.
// A regra de maioridade fica no backend; aqui só se garante uma data de calendário válida.

const PATTERN = /^(\d{2})\/(\d{2})\/(\d{4})$/;

export function parseBirthDate(input: string): string | null {
  const match = PATTERN.exec(input.trim());
  if (match === null) {
    return null;
  }
  const [, dd, mm, yyyy] = match;
  if (dd === undefined || mm === undefined || yyyy === undefined) {
    return null;
  }
  const day = Number(dd);
  const month = Number(mm);
  const year = Number(yyyy);
  const date = new Date(Date.UTC(year, month - 1, day));
  const sameDay =
    date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
  return sameDay ? `${yyyy}-${mm}-${dd}` : null;
}
