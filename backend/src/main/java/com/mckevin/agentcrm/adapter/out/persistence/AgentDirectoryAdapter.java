package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.application.port.AgentAccounts;
import com.mckevin.agentcrm.application.port.AgentDirectory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class AgentDirectoryAdapter implements AgentDirectory, AgentAccounts {

    private final UserSpringRepository users;

    AgentDirectoryAdapter(UserSpringRepository users) {
        this.users = users;
    }

    @Override
    public List<AgentSummary> agents() {
        return users.findAll(Sort.by("fullName")).stream().map(AgentDirectoryAdapter::toSummary).toList();
    }

    @Override
    public Optional<AgentSummary> find(long id) {
        return users.findById(id).map(AgentDirectoryAdapter::toSummary);
    }

    @Override
    public Optional<Credentials> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email).map(u -> new Credentials(u.getId(), u.getFullName(), u.getEmail(),
                u.getPasswordHash(), u.getRole() == UserEntity.Role.MANAGER));
    }

    private static AgentSummary toSummary(UserEntity u) {
        return new AgentSummary(u.getId(), u.getFullName(), u.getEmail(), u.getRole() == UserEntity.Role.MANAGER);
    }
}
