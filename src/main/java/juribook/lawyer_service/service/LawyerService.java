package juribook.lawyer_service.service;

import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.dto.response.LawyerSearchResponse;
import juribook.lawyer_service.dto.response.SpecialtyResponse;
import juribook.lawyer_service.entity.Address;
import juribook.lawyer_service.entity.Lawyer;
import juribook.lawyer_service.entity.Specialty;
import juribook.lawyer_service.event.LawyerEventPublisher;
import juribook.lawyer_service.exception.LawyerProfileAlreadyExistsException;
import juribook.lawyer_service.exception.LawyerProfileNotFoundException;
import juribook.lawyer_service.repository.LawyerRepository;
import juribook.lawyer_service.repository.ReviewRepository;
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
    private final ReviewRepository reviewRepository;
    private final LawyerEventPublisher lawyerEventPublisher;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE     = 50;

    // ── Créer un profil avocat ───────────────────────────────
    @Transactional
    public LawyerProfileResponse createProfile(Long authUserId, String barNumber, String name,
                                               CreateLawyerProfileRequest request) {
        if (lawyerRepository.existsByAuthUserId(authUserId)) {
            throw new LawyerProfileAlreadyExistsException(
                "Un profil existe déjà pour cet avocat"
            );
        }

        List<Specialty> specialties = resolveSpecialties(request.getSpecialtyIds());

        Lawyer lawyer = new Lawyer();
        lawyer.setAuthUserId(authUserId);
        lawyer.setName(name != null ? name : request.getName());
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

        // Pas de publication Kafka ici : available démarre toujours à
        // true à la création, ce n'est pas un "changement" au sens de
        // l'événement lawyer.status-changed.

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

        // Capturé avant modification, pour ne publier l'événement Kafka que si la valeur change réellement.
        boolean previousAvailability = lawyer.isAvailable();

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

        if (request.getAvailable() != null && request.getAvailable() != previousAvailability) {
            lawyerEventPublisher.publishStatusChanged(saved);
        }

        return LawyerProfileResponse.from(saved);
    }

    // ── Recherche paginée avec filtres ───────────────────────
    @Transactional(readOnly = true)
    public Page<LawyerSearchResponse> search(String specialtySlug, String city,
                                             String query, Integer maxRate,
                                             int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize);

        String normalizedSlug  = isBlank(specialtySlug) ? null : specialtySlug.trim().toLowerCase();
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

    // ══════════════════════════════════════════════════════════
    //  Note moyenne
    // ══════════════════════════════════════════════════════════
    /**
     * Recalcule averageRating et reviewCount à partir de TOUS les avis
     * existants pour cet avocat (pas un calcul incrémental), appelé
     * par ReviewService.createReview, dans la même transaction que la
     * sauvegarde de l'avis : si l'un échoue, l'autre est annulé aussi,
     * le profil ne peut jamais afficher une note désynchronisée des
     * avis réellement en base.
     *
     * Recalcul complet plutôt qu'incrémental (ex: maintenir une somme
     * courante) : plus simple, correct par construction, et le volume
     * d'avis par avocat reste largement dans les limites d'un COUNT/AVG
     * SQL trivial pour ce projet, pas besoin d'optimisation prématurée.
     *
     * Arrondi à une décimale pour l'affichage (4.4285714... → 4.4).
     */
    @Transactional
    public void recalculateRating(Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new LawyerProfileNotFoundException(
                    "Avocat introuvable : id=" + lawyerId));

        Double rawAverage = reviewRepository.findAverageRatingByLawyerId(lawyerId);
        long count = reviewRepository.countByLawyerId(lawyerId);

        Double roundedAverage = rawAverage != null
                ? Math.round(rawAverage * 10.0) / 10.0
                : null;

        lawyer.setAverageRating(roundedAverage);
        lawyer.setReviewCount((int) count);

        lawyerRepository.save(lawyer);
        log.info("Note moyenne recalculée : lawyerId={}, averageRating={}, reviewCount={}",
                lawyerId, roundedAverage, count);
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}