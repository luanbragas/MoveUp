-- =====================================================================
-- V22 — Modelo de anamnese v1 do sistema (Fase 5)
--
-- Perguntas de prontidão para atividade física no estilo PAR-Q (texto
-- próprio, em português simples). Qualquer "sim" no grupo parq marca
-- parq_positive e a liberação médica fica pendente (o personal é avisado).
-- As respostas vão cifradas em anamnesis.answers (FieldCipher); objetivo,
-- nível, dias e minutos também vão abertos nas colunas de ação.
-- =====================================================================

insert into anamnesis_template (organization_id, name, version, questions)
values (null, 'padrao', 1, $json$
[
  {"code": "goal", "section": "goal", "type": "single", "required": true,
   "label": "Qual é o seu principal objetivo?",
   "options": [
     {"value": "weight_loss", "label": "Emagrecer"},
     {"value": "hypertrophy", "label": "Ganhar massa muscular"},
     {"value": "conditioning", "label": "Melhorar o condicionamento"},
     {"value": "health", "label": "Saúde e qualidade de vida"},
     {"value": "performance", "label": "Desempenho em um esporte"},
     {"value": "rehab", "label": "Voltar de uma lesão"}]},
  {"code": "goal_detail", "section": "goal", "type": "text", "required": false,
   "label": "Quer contar mais sobre o objetivo?"},
  {"code": "activity_level", "section": "routine", "type": "single", "required": true,
   "label": "Como está sua rotina de exercícios hoje?",
   "options": [
     {"value": "sedentary", "label": "Parado(a) há mais de 3 meses"},
     {"value": "beginner", "label": "Começando agora"},
     {"value": "intermediate", "label": "Treino com regularidade"},
     {"value": "advanced", "label": "Treino há anos, com intensidade"}]},
  {"code": "weekly_days", "section": "routine", "type": "integer", "required": true,
   "label": "Quantos dias por semana pode treinar?", "min": 1, "max": 7},
  {"code": "session_minutes", "section": "routine", "type": "integer", "required": true,
   "label": "Quanto tempo por treino (minutos)?", "min": 15, "max": 240},

  {"code": "parq_heart", "section": "parq", "type": "yes_no", "required": true,
   "label": "Algum médico já disse que você tem um problema no coração e que só deve fazer exercício com orientação médica?"},
  {"code": "parq_chest_exercise", "section": "parq", "type": "yes_no", "required": true,
   "label": "Você sente dor no peito quando faz exercício?"},
  {"code": "parq_chest_rest", "section": "parq", "type": "yes_no", "required": true,
   "label": "No último mês, sentiu dor no peito mesmo parado(a)?"},
  {"code": "parq_dizziness", "section": "parq", "type": "yes_no", "required": true,
   "label": "Você perde o equilíbrio por tontura ou já desmaiou?"},
  {"code": "parq_bone_joint", "section": "parq", "type": "yes_no", "required": true,
   "label": "Tem algum problema nos ossos ou nas articulações que pode piorar com exercício?"},
  {"code": "parq_medication", "section": "parq", "type": "yes_no", "required": true,
   "label": "Toma remédio para pressão ou para o coração?"},
  {"code": "parq_other", "section": "parq", "type": "yes_no", "required": true,
   "label": "Sabe de algum outro motivo para não fazer exercício sem acompanhamento médico?"},

  {"code": "pain_regions", "section": "health", "type": "multi", "required": false,
   "label": "Sente dor em algum lugar hoje?",
   "options": [
     {"value": "neck", "label": "Pescoço"},
     {"value": "shoulder_left", "label": "Ombro esquerdo"},
     {"value": "shoulder_right", "label": "Ombro direito"},
     {"value": "elbow_left", "label": "Cotovelo esquerdo"},
     {"value": "elbow_right", "label": "Cotovelo direito"},
     {"value": "wrist_left", "label": "Punho esquerdo"},
     {"value": "wrist_right", "label": "Punho direito"},
     {"value": "upper_back", "label": "Costas"},
     {"value": "lower_back", "label": "Lombar"},
     {"value": "hip_left", "label": "Quadril esquerdo"},
     {"value": "hip_right", "label": "Quadril direito"},
     {"value": "knee_left", "label": "Joelho esquerdo"},
     {"value": "knee_right", "label": "Joelho direito"},
     {"value": "ankle_left", "label": "Tornozelo esquerdo"},
     {"value": "ankle_right", "label": "Tornozelo direito"}]},
  {"code": "injuries", "section": "health", "type": "text", "required": false,
   "label": "Lesões que já teve (quando e onde)"},
  {"code": "surgeries", "section": "health", "type": "text", "required": false,
   "label": "Cirurgias que já fez"},
  {"code": "conditions", "section": "health", "type": "multi", "required": false,
   "label": "Alguma destas condições?",
   "options": [
     {"value": "hypertension", "label": "Pressão alta"},
     {"value": "diabetes", "label": "Diabetes"},
     {"value": "asthma", "label": "Asma"},
     {"value": "heart", "label": "Doença do coração"},
     {"value": "pregnancy", "label": "Gestação"},
     {"value": "osteoporosis", "label": "Osteoporose"}]},
  {"code": "medications", "section": "health", "type": "text", "required": false,
   "label": "Remédios que usa (nome e para quê)"},
  {"code": "notes", "section": "health", "type": "text", "required": false,
   "label": "Mais alguma coisa que seu personal deva saber?"}
]
$json$::jsonb);
