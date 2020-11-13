
package net.mmo.utils.kism.backend.service;

import net.mmo.utils.kism.backend.repositories.IntermediateNodeRepository;
import net.mmo.utils.kism.backend.repositories.LeafNodeRepository;
import net.mmo.utils.kism.entities.Node;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Companies
 */
@Service
public class NodeService
{
	@SuppressWarnings("unused")
	private IntermediateNodeRepository intermediateNodeRepository;
	@SuppressWarnings("unused")
	private LeafNodeRepository leafNodeRepository;

	public NodeService(IntermediateNodeRepository intermediateNodeRepository,
	                   LeafNodeRepository leafNodeRepository) {
		this.intermediateNodeRepository = intermediateNodeRepository;
		this.leafNodeRepository = leafNodeRepository;
	}

	public Node getRootNode() {
		return null;
	}

	public void save(Node node) {
		// TODO Auto-generated method stub
	}

	public void delete(Node node) {
		// TODO Auto-generated method stub

	}

//	public List<Node> findAll() {
//		return this.nodeRepository.findAll();
//	}
//
//	public List<Node> findAll(String stringFilter) {
//		if (stringFilter == null || stringFilter.isEmpty()) {
//			return this.nodeRepository.findAll();
//		} else {
//			return this.nodeRepository.search(stringFilter);
//		}
//	}

}
