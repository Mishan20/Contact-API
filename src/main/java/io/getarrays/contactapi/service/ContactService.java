package io.getarrays.contactapi.service;

import io.getarrays.contactapi.domain.Contact;
import io.getarrays.contactapi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.getarrays.contactapi.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service // This annotation is used to mark the class as a service
@Slf4j // This annotation is used to provide a logger for the class
@Transactional // This annotation is used to mark the class as a transactional service
@RequiredArgsConstructor // This annotation is used to generate a constructor with required arguments
public class ContactService {
    private final ContactRepo contactRepo; // This annotation is used to inject the ContactRepo class

    public Page<Contact> getAllContacts(int page, int size){
        return contactRepo.findAll(PageRequest.of(page, size, Sort.by("name"))); // This method is used to get all contacts
    }

    public Contact getContact(String id){
        return contactRepo.findById(id).orElseThrow(() -> new RuntimeException("Contact not found")); // This method is used to get a contact by id
    }

    public Contact createContact(Contact contact){
        return contactRepo.save(contact); // This method is used to save a contact
    }

    public void deleteContact(String id){
        contactRepo.deleteById(id); // This method is used to delete a contact
    }

    public String uploadPhoto(String id, MultipartFile file){
        log.info("Saving picture for user ID: {}", id); // This method is used to upload a photo (profile picture
        Contact contact = getContact(id); // This method is used to get a contact by id
        String photoUrl = photoFunction.apply(id, file); // This method is used to store the file
        contact.setPhotoUrl(photoUrl); // This method is used to set the photo URL
        contactRepo.save(contact);  // This method is used to save a contact
        return photoUrl; // This method is used to return the photo URL
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");
    // This method is used to get the file extension
    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/contacts/image/" + filename).toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Could not store file " + image.getOriginalFilename() + ". Please try again!");
        }
    };
    // This method is used to store the file
}
