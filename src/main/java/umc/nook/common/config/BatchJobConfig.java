package umc.nook.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import umc.nook.users.repository.UserRepository;

import java.time.LocalDateTime;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserRepository userRepository;

    @Bean
    public Job purgeDeletedUsersJob() {
        return new JobBuilder("purgeDeletedUsersJob", jobRepository)
                .start(purgeDeletedUsersStep())
                .build();
    }

    @Bean
    public Step purgeDeletedUsersStep() {
        return new StepBuilder("purgeDeletedUsersStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime threshold = LocalDateTime.now().minusDays(30);
                    int deleted = userRepository.hardDeleteUsersOlderThan(threshold);
                    log.info("[Batch] 탈퇴 후 30일 경과 사용자 삭제: " + deleted);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
