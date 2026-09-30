import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  RouterLink
} from '@angular/router';

import {
  Booking,
  BookingService
} from '../../services/booking.service';

import {
  Resource,
  ResourceService
} from '../../services/resource.service';

@Component({
  selector: 'app-bookings',
  imports: [RouterLink],
  templateUrl: './bookings.html',
  styleUrl: './bookings.scss'
})
export class Bookings implements OnInit {

  bookings: Booking[] = [];

  resources: Resource[] = [];

  loading = true;
  error = false;

  cancellingBookingId: number | null = null;

  cancelError = '';

  constructor(
    private bookingService: BookingService,
    private resourceService: ResourceService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {

    this.loading = true;
    this.error = false;

    this.resourceService.getResources().subscribe({

      next: (resources) => {

        this.resources = resources;

        console.log(
          'RESSOURCES CHARGEES :',
          this.resources
        );

        console.log(
          'NOMBRE DE RESSOURCES :',
          this.resources.length
        );

        this.loadBookings();
      },

      error: (error) => {

        console.error(
          'ERREUR CHARGEMENT RESSOURCES :',
          error
        );

        this.loading = false;
        this.error = true;

        this.changeDetectorRef.detectChanges();
      }

    });
  }

  private loadBookings(): void {

    this.bookingService.getMyBookings().subscribe({

      next: (bookings) => {

        this.bookings = bookings;

        this.loading = false;

        console.log(
          'MES RESERVATIONS :',
          this.bookings
        );

        console.log(
          'RESSOURCES DISPONIBLES POUR LES RESERVATIONS :',
          this.resources
        );

        this.changeDetectorRef.detectChanges();
      },

      error: (error) => {

        console.error(
          'ERREUR MES RESERVATIONS :',
          error
        );

        this.loading = false;
        this.error = true;

        this.changeDetectorRef.detectChanges();
      }

    });
  }

  getResourceName(resourceId: number): string {

    const resource = this.resources.find(
      item => item.id === resourceId
    );

    console.log(
      'RECHERCHE RESSOURCE :',
      resourceId,
      resource
    );

    return resource?.name ?? `Ressource #${resourceId}`;
  }

  getResourceType(resourceId: number): string {

    const resource = this.resources.find(
      item => item.id === resourceId
    );

    if (!resource) {
      return '';
    }

    switch (resource.type) {

      case 'ROOM':
        return 'Salle';

      case 'EQUIPMENT':
        return 'Équipement';

      default:
        return 'Praticien';
    }
  }

  cancelBooking(booking: Booking): void {

    if (!this.canCancel(booking)) {
      return;
    }

    this.cancellingBookingId = booking.id;
    this.cancelError = '';

    this.bookingService
      .cancelBooking(booking.id)
      .subscribe({

        next: (cancelledBooking) => {

          this.bookings = this.bookings.map(
            currentBooking =>
              currentBooking.id === cancelledBooking.id
                ? cancelledBooking
                : currentBooking
          );

          this.cancellingBookingId = null;

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'ERREUR ANNULATION RESERVATION :',
            error
          );

          this.cancellingBookingId = null;

          if (error.status === 409) {

            this.cancelError =
              'Cette réservation ne peut plus être annulée.';

          } else if (
            error.status === 401 ||
            error.status === 403
          ) {

            this.cancelError =
              'Votre session a expiré. Veuillez vous reconnecter.';

          } else {

            this.cancelError =
              'Impossible d’annuler la réservation. Veuillez réessayer.';
          }

          this.changeDetectorRef.detectChanges();
        }

      });
  }

  canCancel(booking: Booking): boolean {

    return (
      booking.status === 'PENDING' ||
      booking.status === 'CONFIRMED'
    );
  }

  getStatusLabel(status: string): string {

    switch (status) {

      case 'PENDING':
        return 'En attente';

      case 'CONFIRMED':
        return 'Confirmée';

      case 'CHECKED_IN':
        return 'Présent';

      case 'COMPLETED':
        return 'Terminée';

      case 'CANCELLED':
        return 'Annulée';

      case 'NO_SHOW':
        return 'Absent';

      default:
        return status;
    }
  }

  getStatusClass(status: string): string {

    switch (status) {

      case 'PENDING':
        return 'status-pending';

      case 'CONFIRMED':
        return 'status-confirmed';

      case 'CHECKED_IN':
        return 'status-checked-in';

      case 'COMPLETED':
        return 'status-completed';

      case 'CANCELLED':
        return 'status-cancelled';

      case 'NO_SHOW':
        return 'status-no-show';

      default:
        return '';
    }
  }

  formatDate(date: string): string {

    return new Date(date).toLocaleDateString(
      'fr-FR',
      {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      }
    );
  }

  formatTime(date: string): string {

    return new Date(date).toLocaleTimeString(
      'fr-FR',
      {
        hour: '2-digit',
        minute: '2-digit'
      }
    );
  }
}