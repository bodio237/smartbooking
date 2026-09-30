import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Resource, ResourceService } from '../../services/resource.service';

@Component({
  selector: 'app-resources',
  imports: [RouterLink],
  templateUrl: './resources.html',
  styleUrl: './resources.scss'
})
export class Resources implements OnInit {

  resources: Resource[] = [];
  filteredResources: Resource[] = [];

  loading = true;
  error = false;

  searchTerm = '';
  selectedCategory = 'ALL';

  constructor(
    private resourceService: ResourceService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {

    this.resourceService.getResources().subscribe({

      next: (resources) => {

        console.log('RESSOURCES RECUES :', resources);

        this.resources = resources.filter(resource => resource.isActive);
        this.filteredResources = this.resources;

        this.loading = false;

        console.log('LOADING APRES :', this.loading);
        console.log(
          'RESSOURCES FILTREES :',
          this.filteredResources.length
        );

        this.changeDetectorRef.detectChanges();
      },

      error: (error) => {

        console.error('ERREUR RESSOURCES :', error);

        this.error = true;
        this.loading = false;

        this.changeDetectorRef.detectChanges();
      }

    });

  }

  search(): void {

    const term = this.searchTerm.trim().toLowerCase();

    this.filteredResources = this.resources.filter(resource => {

      const matchesSearch =
        !term ||
        resource.name.toLowerCase().includes(term) ||
        (resource.description?.toLowerCase().includes(term) ?? false) ||
        resource.type.toLowerCase().includes(term);

      const matchesCategory =
        this.selectedCategory === 'ALL' ||
        resource.type === this.selectedCategory;

      return matchesSearch && matchesCategory;
    });
  }

  selectCategory(category: string): void {

    this.selectedCategory = category;
    this.search();
  }

}