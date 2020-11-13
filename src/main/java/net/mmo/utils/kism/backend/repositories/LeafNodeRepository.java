package net.mmo.utils.kism.backend.repositories;

import net.mmo.utils.kism.entities.LeafNode;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository class to access the database. Spring Boot makes this a painless process:
 * All you need to do is define an interface that describes the entity type and primary key type,
 * and Spring Data will configure one for you.
 */
public interface LeafNodeRepository extends JpaRepository<LeafNode, Long>
{
//	// using Java Persistence Query Language (JPQL) to phrase the query:
//	@Query("select n from Node n "
//	      + "where lower(n.name) like lower(concat('%', :searchTerm, '%')) "
//	)
//	List<Node> search(@Param("searchTerm") String searchTerm);
}
