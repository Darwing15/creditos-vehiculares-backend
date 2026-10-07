package pe.sotomayor.creditos.vehicles.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.vehicles.application.dto.request.CreateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.dto.request.UpdateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapper;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;
import pe.sotomayor.creditos.vehicles.domain.service.VehicleCodeGenerator;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleManagementServiceTest {

    static final UUID CODE =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440041");

    @Mock
    VehicleRepository repository;

    @Mock
    CurrentBranchProvider branches;

    @Mock
    VehicleCodeGenerator codes;

    VehicleMapper mapper = Mappers.getMapper(VehicleMapper.class);

    CreateVehicleService create;
    UpdateVehicleService update;
    GetVehicleService get;
    Vehicle current;

    @BeforeEach
    void setUp() {

        create = new CreateVehicleService(
                repository,
                mapper,
                branches,
                codes
        );
        update = new UpdateVehicleService(
                repository,
                mapper,
                branches
        );
        get = new GetVehicleService(
                repository,
                mapper,
                branches
        );

        current = Vehicle.create(
                CODE,
                "MOTO",
                "Bajaj",
                "NS200",
                2026,
                "Negro",
                "CHASSIS-1",
                "ENGINE-1",
                null,
                new BigDecimal("12000.00"),
                3L
        );

        current.setId(8L);

        when(branches.getCurrentBranchId())
                .thenReturn(3L);
    }

    CreateVehicleRequest createRequest() {
        return new CreateVehicleRequest(
                " MOTO ",
                " Bajaj ",
                " NS200 ",
                2026,
                " Negro ",
                " CHASSIS-1 ",
                " ENGINE-1 ",
                null,
                new BigDecimal("12000.00")
        );
    }

    UpdateVehicleRequest updateRequest(String revision) {
        return new UpdateVehicleRequest(
                "MOTO",
                "Bajaj",
                "NS200 actualizado",
                2026,
                "Rojo",
                "CHASSIS-2",
                "ENGINE-2",
                "DUA-2",
                new BigDecimal("13000.00"),
                revision
        );
    }

    @Test
    void createUsesGeneratedIdentitySessionBranchAndInitialState() {

        /* Arrange */

        when(codes.generate()).thenReturn(CODE);
        when(repository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    Vehicle saved = invocation.getArgument(0);
                    saved.setId(9L);
                    return saved;
                });

        /* Act */

        var response = create.create(createRequest());
        var captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(repository).saveAndFlush(captor.capture());
        var saved = captor.getValue();

        /* Assert */

        assertThat(saved.getVehicleCode())
                .isEqualTo(CODE);
        assertThat(saved.getBranchId())
                .isEqualTo(3L);
        assertThat(saved.getChassisNumber())
                .isEqualTo("CHASSIS-1");
        assertThat(saved.getEngineNumber())
                .isEqualTo("ENGINE-1");
        assertThat(saved.getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(response.revision())
                .hasSize(64);
        assertThat(response.id())
                .isEqualTo(9L);
    }

    @Test
    void duplicateCreateStopsBeforeGeneratingIdentityOrWriting() {

        /* Arrange */

        when(repository.existsIdentifiersIncludingDeleted(
                "CHASSIS-1", "ENGINE-1", 0L))
                .thenReturn(true);

        /* Act */

        Throwable throwable = catchThrowable(
                () -> create.create(createRequest())
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_ALREADY_EXISTS);

        verifyNoInteractions(codes);

        verify(repository, never())
                .saveAndFlush(any());
    }

    @Test
    void updatePreservesIdentityAndBranchAndChangesRevision() {

        /* Arrange */

        when(repository.findByVehicleCodeAndBranchIdForUpdate(
                CODE, 3L))
                .thenReturn(Optional.of(current));
        when(repository.saveAndFlush(current))
                .thenReturn(current);

        /* Act */

        String before = mapper.revision(current);
        var response = update.update(CODE, updateRequest(before));

        /* Assert */

        assertThat(current.getModel())
                .isEqualTo("NS200 actualizado");
        assertThat(current.getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(current.getVehicleCode())
                .isEqualTo(CODE);
        assertThat(current.getBranchId())
                .isEqualTo(3L);
        assertThat(current.getId())
                .isEqualTo(8L);
        assertThat(response.revision())
                .isNotEqualTo(before);
    }

    @Test
    void staleFormDoesNotChangeOrSaveRecord() {

        /* Arrange */

        when(repository.findByVehicleCodeAndBranchIdForUpdate(
                CODE, 3L))
                .thenReturn(Optional.of(current));

        /* Act */

        String before = mapper.revision(current);

        Throwable throwable = catchThrowable(
                () -> update.update(CODE, updateRequest("0".repeat(64)))
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);

        assertThat(mapper.revision(current))
                .isEqualTo(before);

        verify(repository, never())
                .saveAndFlush(any());
    }

    @Test
    void updateDoesNotFallBackToUnscopedLookup() {

        /* Arrange */

        /* Act */

        Throwable throwable = catchThrowable(
                () -> update.update(CODE, updateRequest("0".repeat(64)))
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);

        verify(repository).findByVehicleCodeAndBranchIdForUpdate(
                CODE, 3L);

        verify(repository, never())
                .saveAndFlush(any());
    }

    @Test
    void getReadsOnlyFromCurrentBranch() {

        /* Arrange */

        when(repository.findByVehicleCodeAndBranchId(CODE, 3L))
                .thenReturn(Optional.of(current));

        /* Act */

        var response = get.get(CODE);

        /* Assert */

        assertThat(response.vehicleCode())
                .isEqualTo(CODE);
        assertThat(response.revision())
                .isEqualTo(mapper.revision(current));
        verify(repository)
                .findByVehicleCodeAndBranchId(
                        CODE, 3L);
    }

    @Test
    void getHidesMissingDeletedAndOtherBranchResources() {

        /* Arrange */

        /* Act */

        Throwable throwable = catchThrowable(
                () -> get.get(CODE)
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findByVehicleCodeAndBranchId(
                CODE, 3L);
    }

    @Test
    void reservedVehicleCannotBeEditedEvenWithCurrentRevision() {

        /* Arrange */

        current.reserveForFinancing();

        when(repository.findByVehicleCodeAndBranchIdForUpdate(
                CODE, 3L))
                .thenReturn(Optional.of(current));

        /* Act */

        Throwable throwable = catchThrowable(
                () -> update.update(CODE,
                        updateRequest(mapper.revision(current)))
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_EDITABLE);

        verify(repository, never())
                .saveAndFlush(any());

        verify(repository, never())
                .existsIdentifiersIncludingDeleted(
                        any(), any(), any());

        assertThat(current.getModel())
                .isEqualTo("NS200");
    }

    @Test
    void identifiersOwnedByAnotherUnitPreventUpdate() {

        /* Arrange */

        when(repository.findByVehicleCodeAndBranchIdForUpdate(CODE, 3L))
                .thenReturn(Optional.of(current));
        when(repository.existsIdentifiersIncludingDeleted(
                "CHASSIS-2", "ENGINE-2", 8L))
                .thenReturn(true);

        /* Act */

        Throwable throwable = catchThrowable(
                () -> update.update(CODE,
                        updateRequest(mapper.revision(current)))
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_ALREADY_EXISTS);

        verify(repository, never())
                .saveAndFlush(any());

        assertThat(current.getChassisNumber())
                .isEqualTo("CHASSIS-1");
    }

}
