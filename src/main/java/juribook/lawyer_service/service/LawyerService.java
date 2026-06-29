package juribook.lawyer_service.service;

import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.dto.response.LawyerSearchResponse;
import juribook.lawyer_service.dto.response.SpecialtyResponse;
import juribook.lawyer_service.entity.Address;
import juribook.lawyer_service.entity.Lawyer;
import juribook.lawyer_service.entity.Specialty;
import juribook.lawyer_service.exception.LawyerProfileAlreadyExistsException;
import juribook.lawyer_service.exception.LawyerProfileNotFoundException;
import juribook.lawyer_service.repository.LawyerRepository;
import juribook.lawyer_service.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final SpecialtyRepository specialtyRepository;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE     = 50;

    // ── Créer un profil avocat ───────────────────────────────
    @Transactional
    public LawyerProfileResponse createProfile(Long authUserId, String barNumber,
                                               CreateLawyerProfileRequest request) {
        if (lawyerRepository.existsByAuthUserId(authUserId)) {
            throw new LawyerProfileAlreadyExistsException(
                "Un profil existe déjà pour cet avocat"
            );
        }

        List<Specialty> specialties = resolveSpecialties(request.getSpecialtyIds());

        Lawyer lawyer = new Lawyer();
        lawyer.setAuthUserId(authUserId);
        lawyer.setBarNumber(barNumber);
        lawyer.setBio(request.getBio());
        lawyer.setHourlyRate(request.getHourlyRate());
        lawyer.setYearsExperience(request.getYearsExperience());
        lawyer.setLanguages(request.getLanguages());
        lawyer.setSpecialties(specialties);
        lawyer.setAvailable(true);
        lawyer.setAddress(buildAddress(request));

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

        if (request.getSpecialtyIds() != null && !request.getSpecialtyIds().isEmpty()) {
            lawyer.setSpecialties(resolveSpecialties(request.getSpecialtyIds()));
        }

        updateAddress(lawyer, request);

        Lawyer saved = lawyerRepository.save(lawyer);
        log.info("Profil avocat mis à jour : id={}, authUserId={}", saved.getId(), authUserId);

        return LawyerProfileResponse.from(saved);
    }

    // ── Recherche paginée avec filtres ───────────────────────
    /**
     * Recherche d'avocats avec filtres optionnels cumulables.
     *
     * @param specialtySlug  slug de la spécialité (ex: "droit-du-travail") - nullable
     * @param city           ville du cabinet - nullable
     * @param query          recherche textuelle libre - nullable
     * @param maxRate        tarif horaire maximum - nullable
     * @param page           numéro de page (0-based)
     * @param size           taille de page (défaut 20, max 50)
     * @return page de résultats triés par note décroissante
     */
    @Transactional(readOnly = true)
    public Page<LawyerSearchResponse> search(String specialtySlug, String city,
                                             String query, Integer maxRate,
                                             int page, int size) {
        // Limiter la taille de page pour éviter les surcharges
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize);

        // Normaliser les paramètres (null si vide ou blank)
        String normalizedSlug  = isBlank(specialtySlug) ? null : specialtySlug.trim().toLowerCase();
        // La requête JPQL compare l'égalité exacte sur city — on capitalise
        // la première lettre pour matcher "Paris" stocké en BDD
        String normalizedCity  = isBlank(city) ? null : capitalize(city.trim());
        String normalizedQuery = isBlank(query)         ? null : query.trim();

        Page<Lawyer> results = (normalizedQuery != null)
            ? lawyerRepository.searchWithQuery(normalizedSlug, normalizedCity, maxRate, normalizedQuery, pageable)
            : lawyerRepository.search(normalizedSlug, normalizedCity, maxRate, pageable);

        log.debug("Recherche avocats : specialty={}, city={}, query={}, maxRate={} → {} résultats",
            specialtySlug, city, query, maxRate, results.getTotalElements());

        return results.map(LawyerSearchResponse::from);
    }

    // ── Lister toutes les spécialités ───────────────────────
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> getAllSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(SpecialtyResponse::from)
                .toList();
    }

    // ── Helpers privés ───────────────────────────────────────
    private List<Specialty> resolveSpecialties(List<Long> ids) {
        return ids.stream()
                .map(id -> specialtyRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Spécialité introuvable : id=" + id
                        )))
                .toList();
    }

    private Address buildAddress(CreateLawyerProfileRequest request) {
        return new Address(
            request.getStreet(),
            request.getCity(),
            request.getPostalCode(),
            request.getDepartment(),
            request.getRegion()
        );
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

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Capitalise la première lettre et met le reste en minuscules.
     * Ex: "paris" → "Paris", "PARIS" → "Paris"
     * Utilisé pour normaliser la ville avant comparaison en BDD.
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}