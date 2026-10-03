-- Resultado de bloco por tempo identificado pela posição do bloco na versão do treino, não pelo id.
-- Uma versão ainda não usada é substituída no lugar (os blocos ganham ids novos) e o aluno pode ter
-- treinado offline a anterior: a referência por id quebraria o envio. A tabela nunca foi gravada.
alter table block_result drop constraint block_result_pkey;
alter table block_result drop column workout_block_id;
alter table block_result add column block_index smallint not null check (block_index between 0 and 39);
alter table block_result add primary key (session_id, block_index);
