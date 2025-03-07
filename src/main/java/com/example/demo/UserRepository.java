package com.example.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface UserRepository extends JpaRepository<User, Integer> {
	
	User findByEmail(String email); // Add this method
	
	@Query(value="select * from users where username=?1", nativeQuery=true )
	Optional<User> findByUsername(String username);
	
	List<User> findByBanned(boolean banned);
	
	 List<User> findByRole(String role);
	
}