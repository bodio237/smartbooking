import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Resource, ResourceService } from '../../services/resource.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {

  resources: Resource[] = [];
  filteredResources: Resource[] = [];

  loading = true;
  error = false;

  searchTerm = '';
  selectedCategory = 'ALL';

  constructor(private resourceService: ResourceService) {}

  ngOnInit(): void {
    this.resourceService.getResources().subscribe({
      next: (resources) => {
        this.resources = resources.filter(resource => resource.isActive);
        this.filteredResources = this.resources;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
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