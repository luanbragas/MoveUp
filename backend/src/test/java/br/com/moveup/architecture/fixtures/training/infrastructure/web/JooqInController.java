package br.com.moveup.architecture.fixtures.training.infrastructure.web;

import org.jooq.DSLContext;

/** Violação: jOOQ fora de persistence. */
class JooqInController {
  DSLContext dsl;
}
