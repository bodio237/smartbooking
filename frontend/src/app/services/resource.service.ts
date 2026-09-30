import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Resource {
  id: number;
  name: string;
  type: string;
  description: string | null;
  capacity: number;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ResourceService {

  private readonly apiUrl = 'http://localhost:8080/api/resources';

  constructor(private http: HttpClient) {}

  getResources(): Observable<Resource[]> {
    return this.http.get<Resource[]>(this.apiUrl);
  }
}