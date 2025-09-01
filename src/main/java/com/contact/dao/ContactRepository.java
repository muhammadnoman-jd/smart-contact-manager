package com.contact.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.contact.entities.Contact;
import com.contact.entities.User;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
//	if dont want to implement pagination and show all contacts on one page
//	@Query("select c from Contact c where c.user.id =:userId ")
//	public List<Contact> findContactsByUser(@Param("userId") int userId);
	
	@Query("select c from Contact c where c.user.id =:userId ")
	public Page<Contact> findContactsByUser(@Param("userId") int userId,Pageable pageable); //pageable will have currentpage i.e page in my case and contact per page i.e 5 in my case
	
	
	//search
	public List<Contact> findByNameContainingAndUser(String keywords,User user);
}
