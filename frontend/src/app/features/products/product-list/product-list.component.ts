import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';

import { ProductService } from '../services/product.service';
import { ProductSummary, SimpleCatalogEntry } from '../services/product.models';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, DropdownModule, TagModule],
  templateUrl: './product-list.component.html'
})
export class ProductListComponent implements OnInit {

  products: ProductSummary[] = [];
  totalRecords = 0;
  loading = false;

  search = '';
  categoryId: number | null = null;
  categories: SimpleCatalogEntry[] = [];

  private page = 0;
  private readonly pageSize = 15;
  private searchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly productService: ProductService,
    private readonly router: Router,
    protected readonly authService: AuthService
  ) {
  }

  ngOnInit(): void {
    this.productService.categories().subscribe((categories) => (this.categories = categories));
  }

  loadProducts(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    this.productService.search({
      search: this.search || undefined,
      categoryId: this.categoryId,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.products = result.content;
        this.totalRecords = result.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  onFilterChange(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.loadProducts({ first: 0 }), 300);
  }

  goToNew(): void {
    this.router.navigateByUrl('/productos/nuevo');
  }

  goToDetail(product: ProductSummary): void {
    this.router.navigateByUrl(`/productos/${product.id}`);
  }
}
