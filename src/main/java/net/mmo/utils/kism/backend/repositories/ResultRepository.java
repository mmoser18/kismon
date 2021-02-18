/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.repositories;

import java.time.LocalDateTime;
import java.util.List;

import net.mmo.utils.kism.entities.results.RequestResults;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository class to access the database. Spring Boot makes this a painless process:
 * All you need to do is define an interface that describes the entity type and primary key type,
 * and Spring Data will configure one for you.
 */
public interface ResultRepository extends JpaRepository<RequestResults, Long>
{
	// using Java Persistence Query Language (JPQL) to phrase the query:
	@Query("select r from RequestResults r "
	      + "where lower(r.name) like lower(concat('%', :searchTerm, '%'))"
	)
	List<RequestResults> findByName(@Param("searchTerm") String searchName);

	@Query("select r from RequestResults r"
	      + " where (concat('%', :lowerBound, '%') is null or r.duration >= concat('%', :lowerBound, '%'))"
	      + "   and (concat('%', :upperBound, '%') is null or r.duration <= concat('%', :upperBound, '%'))"
	)
	List<RequestResults> findByDuration(@Param("lowerBound") long lowerBound, @Param("upperBound") long upperBound);

	@Query("select r from RequestResults r"
	      + " where (concat('%', :lowerBound, '%') is null or r.timestamp >= concat('%', :lowerBound, '%'))"
	      + "   and (concat('%', :upperBound, '%') is null or r.timestamp <= concat('%', :upperBound, '%'))"
	)
	List<RequestResults> findByTimestamp(@Param("lowerBound") LocalDateTime lowerBound, @Param("upperBound") LocalDateTime upperBound);
}
