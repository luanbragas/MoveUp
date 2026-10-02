import type { ClientAction, LinkStatus } from "../domain/client";

// Textos da feature clients (pt-BR), num lugar só e prontos para i18n.
export const strings = {
  list: {
    title: "Seus alunos",
    count: (n: number) => (n === 1 ? "1 aluno" : `${String(n)} alunos`),
    invite: "Convidar aluno",
    empty: "Você ainda não tem alunos. Convide o primeiro.",
    error: "Não conseguimos carregar seus alunos.",
    retry: "Tentar de novo",
    loadMore: "Carregar mais",
    inviteExpires: (date: string) => `Convite válido até ${date}`,
    noInvite: "Sem convite válido. Reenvie para gerar um novo.",
    since: (date: string) => `Desde ${date}`,
  },
  status: {
    pending: "Pendente",
    active: "Ativo",
    inactive: "Inativo",
  } satisfies Record<LinkStatus, string>,
  actions: {
    share: "Compartilhar convite",
    resend: "Reenviar convite",
    "cancel-invite": "Cancelar convite",
    inactivate: "Inativar",
    reactivate: "Reativar",
    end: "Encerrar",
  } satisfies Record<ClientAction, string>,
  confirmBack: "Voltar",
  confirm: {
    "cancel-invite": {
      title: "Cancelar convite?",
      message: "O código e o link deixam de funcionar. Você pode reenviar depois.",
    },
    inactivate: {
      title: "Inativar aluno?",
      message: "Ele deixa de contar no limite do plano e não recebe treinos até ser reativado.",
    },
    end: {
      title: "Encerrar vínculo?",
      message: "O aluno sai da sua lista. O histórico dele continua guardado na conta dele.",
    },
  },
  form: {
    title: "Convidar aluno",
    subtitle: "Pré-cadastro: o aluno completa o resto ao aceitar o convite.",
    name: "Nome do aluno",
    email: "E-mail (opcional)",
    phone: "WhatsApp com DDD (opcional)",
    phonePlaceholder: "+55 11 98765-4321",
    goal: "Objetivo (opcional)",
    submit: "Criar convite",
    cancel: "Cancelar",
    nameInvalid: "Informe um nome entre 2 e 200 caracteres.",
    emailInvalid: "Confira o e-mail.",
    phoneInvalid: "Confira o WhatsApp, com DDD.",
    goalTooLong: "O objetivo pode ter até 500 caracteres.",
  },
  share: {
    title: "Convite pronto",
    subtitle: (name: string | null) =>
      name === null
        ? "Envie o link ou mostre o QR Code ao aluno."
        : `Envie o link ou mostre o QR Code para ${name}.`,
    code: "Código",
    expires: (date: string) => `Válido até ${date}. Uso único.`,
    qrLabel: "QR Code do convite",
    whatsapp: "Enviar pelo WhatsApp",
    shareOther: "Compartilhar",
    done: "Concluir",
    whatsappFailed: "Não foi possível abrir o WhatsApp. Use o botão Compartilhar.",
    invalid: "Convite inválido. Volte para a lista de alunos.",
  },
} as const;
