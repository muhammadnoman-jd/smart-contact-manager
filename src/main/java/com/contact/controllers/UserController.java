package com.contact.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.contact.dao.ContactRepository;
import com.contact.dao.UserRepository;
import com.contact.entities.Contact;
import com.contact.entities.User;
import com.contact.helper.MessageHelper;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;



@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@ModelAttribute   //common method that will execute in every controller dont have to write in every controller again and again
	public void addCommondata(Model m,Principal principal) {
		String name = principal.getName();  //username/email
		System.out.println(name);
		User userByName = this.userRepository.getUserByName(name);
		System.out.println(userByName);
		m.addAttribute("user", userByName);
	}
	
	@RequestMapping("dashboard")
	public String dashboard(Model m,Principal principal) {    //Principal to send the data username etc to dashboard and then we can get other details by providing that user name to repository and send my Model
		
		m.addAttribute("title", "Dashboard");
		return "user/userdashboard";
	}
	
	@GetMapping("/addcontact")
	public String addContact(Model m, HttpSession session) {
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact", new Contact());
		 MessageHelper message = (MessageHelper) session.getAttribute("message");
	        if (message != null) {
	            m.addAttribute("message", message);
	            session.removeAttribute("message");
	        }
		return "/user/addcontact";
	}
	
	@PostMapping("/process-contact")  //here i am taking image through requestparam because i have taken string imaage property and i am only saving url/name of img in database i could have directly saved the image in database but thats not considered best practice because there can be too large files thats why and remaining fields through modelattribute and saving that to user  
	public String processContact(
	        @ModelAttribute Contact contact,
	        @RequestParam("imageInput") MultipartFile file,
	        Principal principal,HttpSession session) {

	    try {
	        // Save uploaded file
	        if (file.isEmpty()){
	        	System.out.println("image not uploaded");
	        	contact.setImage("default.png");
	        	
	        } else{
	            contact.setImage(file.getOriginalFilename());
	            File f = new ClassPathResource("static/img").getFile();
	            Files.copy(file.getInputStream(),
	                    Paths.get(f.getAbsolutePath() + File.separator + file.getOriginalFilename()),
	                    StandardCopyOption.REPLACE_EXISTING);
	        }

	        // Set contact owner
	        String name = principal.getName();
	        User user = this.userRepository.getUserByName(name);
	        contact.setUser(user);
	        user.getContacts().add(contact);
	        this.userRepository.save(user);

	        System.out.println("Added contact: " + contact);
	        session.setAttribute("message", new MessageHelper("Contact added successfully!", "alert-success"));
	    } catch (Exception e) {
	        e.printStackTrace();
	        session.setAttribute("message", new MessageHelper("Something went wrong!", "alert-danger"));
	    }

	    return "/user/addcontact";
	}
	
//	showcontactshandler

	@RequestMapping("/showcontacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal,HttpSession session) {
		//could have done using princial and get contacts from getting username but i had to do pagination so for clarity made contactrepository instead
		
		m.addAttribute("title", "My Contacts");
		String name = principal.getName();
		User user = this.userRepository.getUserByName(name);
		int id = user.getId();
		
//		per page = 5[n]
//		currentpage = 0[page]
		PageRequest of = PageRequest.of(page,10);  //pagerequest that is child class of pageable interface
		Page<Contact> contacts = this.contactRepository.findContactsByUser(id,of);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		session.removeAttribute("message");
		
		return "user/showcontacts";
	}

//	showcontactdetails handler
	@RequestMapping("/{cid}/contact")
	public String showContactDetails(@PathVariable("cid")Integer cid,Model m,Principal principal) {
		m.addAttribute("title","Contact Details");
		Optional<Contact> contactdetails = this.contactRepository.findById(cid);
		if (contactdetails.isPresent()) {
	        Contact contact = contactdetails.get();
	        String name = principal.getName();
	        User user = this.userRepository.getUserByName(name);

	        if (user.getId() == contact.getUser().getId()) {
	            m.addAttribute("contact", contact);
	            m.addAttribute("hasPermission", true);
	        } else {
	            m.addAttribute("hasPermission", false);
	        }
	    } else {
	        m.addAttribute("hasPermission", false);
	    }
		
		return "user/showcontactdetails";
	}
	
//	deletecontacthandler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid")Integer cid,Model m,Principal principal,HttpSession session) {
		Contact contact = this.contactRepository.findById(cid).get();
//		contact.setUser(null); //because in entity i have cascaded all due to which it was not deleted so i unlinked here but this will only unlink and wcant be seen here cuz its not associated or link to any user but it will be there in database still so better use orphanRemoval =true in user or to set in repo with transaction anno
		String name = principal.getName();
		User user = this.userRepository.getUserByName(name);
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		
		
		return "redirect:/user/showcontacts/0";
		
	}
	
//	open edit contact handler
	@PostMapping("/editcontact/{cid}")
	public String editContact(@PathVariable("cid")Integer cid,Model m,HttpSession session) {
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("title", "Edit Contact");
		m.addAttribute("contact", contact);
		if (session.isNew()) {
			session.setAttribute("message", new MessageHelper("Updated Successfully", "alert-success"));
		}
		return "user/updateform";
		
	}
	
	//process edit contact handler
	@PostMapping("/process-update")
	public String editContactProcess( @ModelAttribute Contact contact,
	        @RequestParam("imageInput") MultipartFile file,
	        Principal principal,HttpSession session) {
		Contact olddetails = this.contactRepository.findById(contact.getCid()).get();
			try {
				if (file.isEmpty()){
		        	System.out.println("image not uploaded");
		        	contact.setImage(olddetails.getImage());
		        	
		        }else{
		            contact.setImage(file.getOriginalFilename());
		            File f = new ClassPathResource("static/img").getFile();
		            Files.copy(file.getInputStream(),
		                    Paths.get(f.getAbsolutePath() + File.separator + file.getOriginalFilename()),
		                    StandardCopyOption.REPLACE_EXISTING);
		        }
				User userByName = this.userRepository.getUserByName(principal.getName());
				contact.setUser(userByName);
				this.contactRepository.save(contact);
				session.setAttribute("message", new MessageHelper("Updated Succesfully", "alert-success"));
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		return "redirect:/user/"+contact.getCid()+"/contact";
	}
	
	@GetMapping("/profile")
	public String userProfile(Model m) {
		m.addAttribute("title","Profile");
		return "user/userprofile";
	}
	
	@GetMapping("/settings")
	public String settings() {
		return("/user/settings");
	}
	
	@PostMapping("/change-password")
	public String changePass(@RequestParam("oldPassword") String oldPass, @RequestParam("newPassword")String newPass,Principal principal,RedirectAttributes redirectAttributes) {
		User user = this.userRepository.getUserByName(principal.getName());
		if (this.bCryptPasswordEncoder.matches(oldPass, user.getPassword())) {
			user.setPassword(this.bCryptPasswordEncoder.encode(newPass));
			this.userRepository.save(user);
			redirectAttributes.addFlashAttribute("message", 
		            new MessageHelper("Changed password successfully!", "alert-success"));
		} else {
			redirectAttributes.addFlashAttribute("message", 
		            new MessageHelper("Wrong Credentials!", "alert-danger"));

		}
		return("redirect:/user/settings");
	}
}
