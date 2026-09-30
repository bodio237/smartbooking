import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';

import { AuthService } from '../../services/auth.service';

import {
  Booking,
  BookingService
} from '../../services/booking.service';

import {
  Resource,
  ResourceService
} from '../../services/resource.service';


@Component({
  selector: 'app-admin',

  imports: [
    CommonModule,
    RouterLink
  ],

  templateUrl: './admin.html',
  styleUrl: './admin.scss'
})
export class Admin implements OnInit {

  resources: Resource[] = [];
  bookings: Booking[] = [];

  loading = true;
  error = '';

  updatingBookingId: number | null = null;
  updateError = '';

  constructor(
    private authService: AuthService,
    private resourceService: ResourceService,
    private bookingService: BookingService,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {

    if (this.authService.getUserRole() !== 'ADMIN') {
      this.router.navigate(['/home']);
      return;
    }

    this.loadDashboard();
  }


  loadDashboard(): void {

    this.loading = true;
    this.error = '';
    this.updateError = '';
    this.resources = [];
    this.bookings = [];

    this.resourceService
      .getResources()
      .pipe(

        switchMap(resources => {

          this.resources = resources.filter(
            resource => resource.isActive
          );

          if (this.resources.length === 0) {
            return of([] as Booking[]);
          }

          const bookingRequests =
            this.resources.map(resource => {

              return this.bookingService
                .getResourceBookings(resource.id)
                .pipe(
                  catchError(() => {
                    return of([] as Booking[]);
                  })
                );
            });

          return forkJoin(bookingRequests);
        }),

        catchError(() => {

          this.error =
            'Impossible de charger le tableau de bord.';

          return of([] as Booking[]);
        }),

        finalize(() => {

          this.loading = false;

          this.changeDetectorRef.detectChanges();
        })

      )
      .subscribe({

        next: bookingLists => {

          const allBookings =
            bookingLists.flat();

          this.bookings =
            this.removeDuplicateBookings(
              allBookings
            );

          this.changeDetectorRef.detectChanges();
        },

        error: () => {

          this.loading = false;

          this.error =
            'Impossible de charger le tableau de bord.';

          this.changeDetectorRef.detectChanges();
        }

      });
  }


  private removeDuplicateBookings(
    bookings: Booking[]
  ): Booking[] {

    const uniqueBookings =
      new Map<number, Booking>();

    bookings.forEach(booking => {

      uniqueBookings.set(
        booking.id,
        booking
      );

    });

    return Array.from(
      uniqueBookings.values()
    ).sort(
      (a, b) => b.id - a.id
    );
  }


  getResourceName(
    resourceId: number
  ): string {

    const resource =
      this.resources.find(
        item => item.id === resourceId
      );

    return resource
      ? resource.name
      : `Ressource #${resourceId}`;
  }


  getResourceType(
    resourceId: number
  ): string {

    const resource =
      this.resources.find(
        item => item.id === resourceId
      );

    return resource
      ? this.formatResourceType(resource.type)
      : 'Ressource';
  }


  private formatResourceType(
    type: string
  ): string {

    switch (type) {

      case 'ROOM':
        return 'Salle';

      case 'EQUIPMENT':
        return 'Équipement';

      case 'PRACTITIONER':
        return 'Praticien';

      default:
        return type;
    }
  }


  get activeResourcesCount(): number {

    return this.resources.filter(
      resource => resource.isActive
    ).length;
  }


  get totalBookingsCount(): number {

    return this.bookings.length;
  }


  get completedBookingsCount(): number {

    return this.bookings.filter(
      booking =>
        booking.status === 'COMPLETED'
    ).length;
  }


  get cancelledBookingsCount(): number {

    return this.bookings.filter(
      booking =>
        booking.status === 'CANCELLED'
    ).length;
  }


  get noShowBookingsCount(): number {

    return this.bookings.filter(
      booking =>
        booking.status === 'NO_SHOW'
    ).length;
  }


  get occupancyRate(): number {

    const activeResources =
      this.activeResourcesCount;

    if (activeResources === 0) {
      return 0;
    }

    const activeBookings =
      this.bookings.filter(
        booking =>
          booking.status !== 'CANCELLED' &&
          booking.status !== 'NO_SHOW'
      ).length;

    return Math.min(
      100,
      Math.round(
        (activeBookings / activeResources) * 100
      )
    );
  }


  get noShowRate(): number {

    if (this.bookings.length === 0) {
      return 0;
    }

    return Math.round(
      (this.noShowBookingsCount /
        this.bookings.length) * 100
    );
  }


  get recentBookings(): Booking[] {

    return [...this.bookings]
      .sort((a, b) => {

        const dateA =
          new Date(a.startTime).getTime();

        const dateB =
          new Date(b.startTime).getTime();

        return dateB - dateA;
      })
      .slice(0, 8);
  }


  getStatusLabel(
    status: string
  ): string {

    switch (status) {

      case 'PENDING':
        return 'En attente';

      case 'CONFIRMED':
        return 'Confirmée';

      case 'CHECKED_IN':
        return 'Arrivée';

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


  getStatusClass(
    status: string
  ): string {

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


  formatDate(
    date: string
  ): string {

    return new Intl.DateTimeFormat(
      'fr-FR',
      {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      }
    ).format(new Date(date));
  }


  formatTime(
    date: string
  ): string {

    return new Intl.DateTimeFormat(
      'fr-FR',
      {
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(new Date(date));
  }


  canChangeStatus(
    booking: Booking
  ): boolean {

    return (
      booking.status !== 'COMPLETED' &&
      booking.status !== 'CANCELLED' &&
      booking.status !== 'NO_SHOW'
    );
  }


  getAvailableStatuses(
    booking: Booking
  ): string[] {

    switch (booking.status) {

      case 'PENDING':
        return [
          'CONFIRMED',
          'CANCELLED'
        ];

      case 'CONFIRMED':
        return [
          'CHECKED_IN',
          'CANCELLED',
          'NO_SHOW'
        ];

      case 'CHECKED_IN':
        return [
          'COMPLETED'
        ];

      default:
        return [];
    }
  }


  changeStatus(
    booking: Booking,
    status: string
  ): void {

    this.updateError = '';
    this.updatingBookingId = booking.id;

    this.bookingService
      .changeStatus(
        booking.id,
        status
      )
      .subscribe({

        next: updatedBooking => {

          const index =
            this.bookings.findIndex(
              item =>
                item.id === updatedBooking.id
            );

          if (index !== -1) {

            this.bookings[index] =
              updatedBooking;
          }

          this.updatingBookingId = null;

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          this.updatingBookingId = null;

          if (error.status === 409) {

            this.updateError =
              'Cette transition de statut est impossible.';

          } else if (
            error.status === 401 ||
            error.status === 403
          ) {

            this.updateError =
              'Vous n’avez pas les droits nécessaires.';

          } else {

            this.updateError =
              'Impossible de modifier le statut.';
          }

          this.changeDetectorRef.detectChanges();
        }

      });
  }


  logout(): void {

    this.authService.logout();

    this.router.navigate(['/login']);
  }
}