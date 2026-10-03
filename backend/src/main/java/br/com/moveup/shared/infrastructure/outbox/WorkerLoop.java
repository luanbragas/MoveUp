package br.com.moveup.shared.infrastructure.outbox;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Laço contínuo do worker numa thread própria: roda o passo (outbox, fila de push) enquanto houver
 * trabalho e espera um pouco quando a fila esvazia. Erro inesperado não derruba o laço.
 */
public class WorkerLoop implements SmartLifecycle {

  private static final Logger LOG = LoggerFactory.getLogger(WorkerLoop.class);

  private final String name;
  private final IntSupplier step;
  private final Duration idle;
  private ExecutorService executor;
  private volatile boolean running;

  /**
   * @param step faz uma rodada e devolve quanto trabalho pegou (0 = espera {@code idle})
   */
  public WorkerLoop(String name, IntSupplier step, Duration idle) {
    this.name = name;
    this.step = step;
    this.idle = idle;
  }

  @Override
  public void start() {
    running = true;
    executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "worker-" + name));
    executor.submit(this::run);
  }

  private void run() {
    while (running) {
      try {
        if (step.getAsInt() == 0) {
          Thread.sleep(idle);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (RuntimeException e) {
        LOG.error("worker_loop_error loop={} error={}", name, e.getClass().getSimpleName());
        try {
          Thread.sleep(idle.multipliedBy(5));
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  @Override
  public void stop() {
    running = false;
    if (executor != null) {
      executor.shutdownNow();
      try {
        executor.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
