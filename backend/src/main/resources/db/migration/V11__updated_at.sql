-- =====================================================================
-- V11 — updated_at automático em toda tabela que tem a coluna
-- =====================================================================

do $$
declare t text;
begin
  for t in
    select c.table_name from information_schema.columns c
      join information_schema.tables tb
        on tb.table_schema = c.table_schema and tb.table_name = c.table_name
     where c.table_schema = 'public' and c.column_name = 'updated_at'
       and tb.table_type = 'BASE TABLE'
  loop
    execute format(
      'create trigger %I before update on %I for each row execute function set_updated_at()',
      t || '_updated_at', t);
  end loop;
end $$;
