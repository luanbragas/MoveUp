package br.com.moveup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada único. O papel do processo vem do perfil: {@code api} (HTTP) ou {@code worker}
 * (outbox e jobs), ver ARQUITETURA.md seção 4.
 */
@SpringBootApplication
public class MoveUpApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoveUpApplication.class, args);
  }
}
