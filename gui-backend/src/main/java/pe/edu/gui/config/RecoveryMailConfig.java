package pe.edu.gui.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class RecoveryMailConfig {
  @Bean(name = "passwordResetMailExecutor")
  public Executor executor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("gui-recovery-mail-");
    executor.initialize();
    return executor;
  }
}
