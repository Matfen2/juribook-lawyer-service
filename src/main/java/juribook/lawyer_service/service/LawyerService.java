package juribook.lawyer_service.service;

import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.entity.Address;
import juribook.lawyer_service.entity.Lawyer;
import juribook.lawyer_service.exception.LawyerProfileAlreadyExistsException;
import juribook.lawyer_service.exception.LawyerProfileNotFoundException;
import juribook.lawyer_service.repository.LawyerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service métier pour les profils avocats.
 *
 * Règles métier :
 *   - Un avocat ne peut avoir qu'un seul profil (unicité par authUserId)
 *   - L'authUserId vient toujours du JWT (jamais du body de la requête)
 *   - Les spécialités doivent exister en BDD (vérification par ID)
 *   - La recherche ne retourne que les avocats disponibles (available = true)
 *   - Pagination : 20 résultats par page par défaut, max 50
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LawyerService {

    private final LawyerRepository lawyerRepository;

    // ── Créer un profil avocat ───────────────────────────────
    @Transactional
    public LawyerProfileResponse createProfile(Long authUserId, String barNumber,
                                               CreateLawyerProfileRequest request) {
        if (lawyerRepository.existsByAuthUserId(authUserId)) {
            throw new LawyerProfileAlreadyExistsException(
                "Un profil existe déjà pour cet avocat"
            );
        }


        Lawyer lawyer = new Lawyer();
        lawyer.setAuthUserId(authUserId);
        lawyer.setBarNumber(barNumber);
        lawyer.setBio(request.getBio());
        lawyer.setHourlyRate(request.getHourlyRate());
        lawyer.setYearsExperience(request.getYearsExperience());
        lawyer.setLanguages(request.getLanguages());
        lawyer.setAvailable(true);

        Lawyer saved = lawyerRepository.save(lawyer);
        log.info("Profil avocat créé : id={}, authUserId={}", saved.getId(), authUserId);

        return LawyerProfileResponse.from(saved);
    }

    // ── Récupérer le profil de l'avocat connecté ────────────
    @Transactional(readOnly = true)
    public LawyerProfileResponse getMyProfile(Long authUserId) {
        Lawyer lawyer = lawyerRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new LawyerProfileNotFoundException(
                    "Profil introuvable — veuillez d'abord créer votre profil"
                ));
        return LawyerProfileResponse.from(lawyer);
    }

    // ── Récupérer un profil public par ID ───────────────────
    @Transactional(readOnly = true)
    public LawyerProfileResponse getProfileById(Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new LawyerProfileNotFoundException(
                    "Avocat introuvable"
                ));
        return LawyerProfileResponse.from(lawyer);
    }

    // ── Mettre à jour le profil ──────────────────────────────
    @Transactional
    public LawyerProfileResponse updateProfile(Long authUserId,
                                               UpdateLawyerProfileRequest request) {
        Lawyer lawyer = lawyerRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new LawyerProfileNotFoundException(
                    "Profil introuvable — veuillez d'abord créer votre profil"
                ));

        if (request.getBio() != null)             lawyer.setBio(request.getBio());
        if (request.getHourlyRate() != null)      lawyer.setHourlyRate(request.getHourlyRate());
        if (request.getYearsExperience() != null) lawyer.setYearsExperience(request.getYearsExperience());
        if (request.getLanguages() != null)       lawyer.setLanguages(request.getLanguages());
        if (request.getAvailable() != null)       lawyer.setAvailable(request.getAvailable());

        updateAddress(lawyer, request);

        Lawyer saved = lawyerRepository.save(lawyer);
        log.info("Profil avocat mis à jour : id={}, authUserId={}", saved.getId(), authUserId);

        return LawyerProfileResponse.from(saved);
    }

    private void updateAddress(Lawyer lawyer, UpdateLawyerProfileRequest request) {
        Address address = lawyer.getAddress() != null
                ? lawyer.getAddress()
                : new Address();

        if (request.getStreet() != null)     address.setStreet(request.getStreet());
        if (request.getCity() != null)       address.setCity(request.getCity());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getDepartment() != null) address.setDepartment(request.getDepartment());
        if (request.getRegion() != null)     address.setRegion(request.getRegion());

        lawyer.setAddress(address);
    }
}