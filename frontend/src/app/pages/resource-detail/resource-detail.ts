import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import {
  FormsModule
} from '@angular/forms';

import {
  Resource,
  ResourceService
} from '../../services/resource.service';

import {
  Booking,
  BookingService,
  CreateBookingRequest
} from '../../services/booking.service';

interface TimeSlot {
  startTime: string;
  endTime: string;
  label: string;
  available: boolean;
}

@Component({
  selector: 'app-resource-detail',
  imports: [
    RouterLink,
    FormsModule
  ],
  templateUrl: './resource-detail.html',
  styleUrl: './resource-detail.scss'
})
export class ResourceDetail implements OnInit {

  resource: Resource | null = null;

  bookings: Booking[] = [];

  selectedDate = '';

  timeSlots: TimeSlot[] = [];

  selectedSlot: TimeSlot | null = null;

  loading = true;
  bookingsLoading = true;
  bookingLoading = false;

  error = false;
  bookingsError = false;
  bookingError = '';

  bookingSuccess = false;

  constructor(
    private route: ActivatedRoute,
    private resourceService: ResourceService,
    private bookingService: BookingService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {

    const id = Number(
      this.route.snapshot.paramMap.get('id')
    );

    this.selectedDate = this.getTodayAsInputValue();

    this.generateTimeSlots();

    this.resourceService.getResources().subscribe({

      next: (resources) => {

        this.resource = resources.find(
          resource => resource.id === id
        ) ?? null;

        this.loading = false;

        console.log(
          'RESSOURCE TROUVEE :',
          this.resource
        );

        if (!this.resource) {
          this.error = true;
          this.changeDetectorRef.detectChanges();
          return;
        }

        this.loadBookings(id);

        this.changeDetectorRef.detectChanges();
      },

      error: (error) => {

        console.error(
          'ERREUR DETAIL RESSOURCE :',
          error
        );

        this.error = true;
        this.loading = false;

        this.changeDetectorRef.detectChanges();
      }

    });
  }

  private loadBookings(resourceId: number): void {

    this.bookingsLoading = true;
    this.bookingsError = false;

    this.bookingService
      .getResourceBookings(resourceId)
      .subscribe({

        next: (bookings) => {

          this.bookings = bookings;
          this.bookingsLoading = false;

          console.log(
            'RESERVATIONS DE LA RESSOURCE :',
            this.bookings
          );

          console.log(
            'NOMBRE DE RESERVATIONS :',
            this.bookings.length
          );

          this.updateSlotAvailability();

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'ERREUR RESERVATIONS RESSOURCE :',
            error
          );

          this.bookingsError = true;
          this.bookingsLoading = false;

          this.changeDetectorRef.detectChanges();
        }

      });
  }

  onDateChange(): void {

    this.selectedSlot = null;
    this.bookingSuccess = false;
    this.bookingError = '';

    this.updateSlotAvailability();
  }

  selectSlot(slot: TimeSlot): void {

    if (!slot.available || this.bookingLoading) {
      return;
    }

    this.selectedSlot = slot;
    this.bookingSuccess = false;
    this.bookingError = '';
  }

  isSlotSelected(slot: TimeSlot): boolean {

    return this.selectedSlot === slot;
  }

  createBooking(): void {

    if (
      !this.resource ||
      !this.selectedSlot ||
      this.bookingLoading
    ) {
      return;
    }

    this.bookingLoading = true;
    this.bookingError = '';
    this.bookingSuccess = false;

    const startTime =
      `${this.selectedDate}T${this.selectedSlot.startTime}:00`;

    const endTime =
      `${this.selectedDate}T${this.selectedSlot.endTime}:00`;

    const request: CreateBookingRequest = {
      resourceId: this.resource.id,
      startTime,
      endTime
    };

    const idempotencyKey = this.generateIdempotencyKey();

    console.log(
      'CREATION RESERVATION :',
      request
    );

    console.log(
      'IDEMPOTENCY KEY :',
      idempotencyKey
    );

    this.bookingService
      .createBooking(
        request,
        idempotencyKey
      )
      .subscribe({

        next: (booking) => {

          console.log(
            'RESERVATION CREEE :',
            booking
          );

          this.bookingLoading = false;
          this.bookingSuccess = true;

          this.bookings = [
            ...this.bookings,
            booking
          ];

          this.selectedSlot = null;

          this.updateSlotAvailability();

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'ERREUR CREATION RESERVATION :',
            error
          );

          this.bookingLoading = false;

          if (error.status === 409) {

            this.bookingError =
              'Ce créneau vient d’être réservé par une autre personne.';

            this.loadBookings(this.resource!.id);

          } else if (error.status === 401 || error.status === 403) {

            this.bookingError =
              'Votre session a expiré. Veuillez vous reconnecter.';

          } else {

            this.bookingError =
              'Impossible de créer la réservation. Veuillez réessayer.';
          }

          this.changeDetectorRef.detectChanges();
        }

      });
  }

  private generateIdempotencyKey(): string {

    if (
      typeof crypto !== 'undefined' &&
      typeof crypto.randomUUID === 'function'
    ) {
      return crypto.randomUUID();
    }

    return `${Date.now()}-${Math.random()
      .toString(36)
      .substring(2, 15)}`;
  }

  private generateTimeSlots(): void {

    const slots: TimeSlot[] = [];

    for (let hour = 8; hour < 18; hour++) {

      const startTime =
        `${this.pad(hour)}:00`;

      const endTime =
        `${this.pad(hour + 1)}:00`;

      slots.push({
        startTime,
        endTime,
        label: `${startTime} – ${endTime}`,
        available: true
      });
    }

    this.timeSlots = slots;
  }

  private updateSlotAvailability(): void {

    if (!this.selectedDate) {
      return;
    }

    this.timeSlots = this.timeSlots.map(slot => {

      const slotStart = new Date(
        `${this.selectedDate}T${slot.startTime}:00`
      );

      const slotEnd = new Date(
        `${this.selectedDate}T${slot.endTime}:00`
      );

      const occupied = this.bookings.some(booking => {

        if (
          booking.status === 'CANCELLED' ||
          booking.status === 'NO_SHOW'
        ) {
          return false;
        }

        const bookingStart =
          new Date(booking.startTime);

        const bookingEnd =
          new Date(booking.endTime);

        return (
          bookingStart < slotEnd &&
          bookingEnd > slotStart
        );
      });

      return {
        ...slot,
        available: !occupied
      };
    });

    if (
      this.selectedSlot &&
      !this.timeSlots.some(
        slot =>
          slot.startTime === this.selectedSlot?.startTime &&
          slot.endTime === this.selectedSlot?.endTime &&
          slot.available
      )
    ) {
      this.selectedSlot = null;
    }
  }

  private getTodayAsInputValue(): string {

    const today = new Date();

    const year = today.getFullYear();

    const month = this.pad(
      today.getMonth() + 1
    );

    const day = this.pad(
      today.getDate()
    );

    return `${year}-${month}-${day}`;
  }

  private pad(value: number): string {

    return value
      .toString()
      .padStart(2, '0');
  }
}