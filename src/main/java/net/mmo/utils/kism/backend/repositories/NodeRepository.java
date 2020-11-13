package net.mmo.utils.kism.backend.repositories;

import java.util.List;

import net.mmo.utils.kism.entities.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository class to access the database. Spring Boot makes this a painless process:
 * All you need to do is define an interface that describes the entity type and primary key type,
 * and Spring Data will configure one for you.
 */
public interface NodeRepository extends JpaRepository<Node, Long>
{
	// using Java Persistence Query Language (JPQL) to phrase the query:
	@Query("select n from Node n "
	      + "where lower(n.name) like lower(concat('%', :searchTerm, '%')) "
	)
	List<Node> search(@Param("searchTerm") String searchTerm);
}
