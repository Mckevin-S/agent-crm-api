package com.mckevin.agentcrm.domain;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.interaction.Channel;
import com.mckevin.agentcrm.domain.interaction.Direction;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.shared.BusinessRuleViolation;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import com.mckevin.agentcrm.domain.task.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.mckevin.agentcrm.support.Fixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowUpTaskTest {

    private static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");
    private static final CustomerId ID = new CustomerId("C-10001");

    private FollowUpTask task() {
        return FollowUpTask.open(ID, 7, "  Rappeler le client ", null, Priority.MEDIUM, TODAY.plusDays(3), null, null, TODAY, NOW);
    }

    @Test
    void lifecycle() {
        FollowUpTask task = task();
        assertThat(task.getTitle()).isEqualTo("Rappeler le client");
        assertThat(task.isOverdue(TODAY.plusDays(4))).isTrue();

        task.reschedule(TODAY.plusDays(10), TODAY);
        assertThat(task.isOverdue(TODAY.plusDays(4))).isFalse();

        task.complete(NOW);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.isOverdue(TODAY.plusDays(30))).isFalse();
    }

    @Test
    void dueDateCannotBeInThePast() {
        assertThatThrownBy(() -> FollowUpTask.open(ID, 7, "Titre", null, Priority.LOW, TODAY.minusDays(1), null, null, TODAY, NOW))
                .isInstanceOf(BusinessRuleViolation.class).extracting("code").isEqualTo("DUE_DATE_IN_PAST");
        FollowUpTask task = task();
        assertThatThrownBy(() -> task.reschedule(TODAY.minusDays(1), TODAY)).extracting("code").isEqualTo("DUE_DATE_IN_PAST");
    }

    @Test
    void closedTaskCannotChange() {
        FollowUpTask task = task();
        task.complete(NOW);
        assertThatThrownBy(() -> task.complete(NOW)).extracting("code").isEqualTo("TASK_NOT_OPEN");
        assertThatThrownBy(() -> task.reschedule(TODAY.plusDays(5), TODAY)).extracting("code").isEqualTo("TASK_NOT_OPEN");
    }

    @Test
    void interactionCannotBeInTheFuture() {
        assertThatThrownBy(() -> Interaction.log(ID, 7, "Conseiller", Channel.PHONE, Direction.OUTBOUND, "Appel", null,
                NOW.plusSeconds(3600), NOW)).extracting("code").isEqualTo("INTERACTION_IN_FUTURE");
        assertThatThrownBy(() -> Interaction.log(ID, 7, "Conseiller", Channel.PHONE, Direction.OUTBOUND, " ", null, NOW, NOW))
                .extracting("code").isEqualTo("INVALID_SUBJECT");
    }

    @Test
    void customerIdFormat() {
        assertThatThrownBy(() -> new CustomerId("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new CustomerId("C-10234").value()).isEqualTo("C-10234");
    }
}
