package com.mckevin.agentcrm.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SnapshotSpringRepository extends JpaRepository<SnapshotEntity, String> {

    @Query("select s from SnapshotEntity s where s.searchText like :pattern order by s.displayName")
    List<SnapshotEntity> search(@Param("pattern") String pattern);
}
