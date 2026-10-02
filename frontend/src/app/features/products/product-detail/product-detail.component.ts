import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { ProductService } from '../services/product.service';
import { AuthService } from '../../../core/services/auth.service';
import { Presentation, PriceHistoryEntry, ProductDetail, UnitOption } from '../services/product.models';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, CardModule, TableModule, TagModule, ButtonModule,
    DialogModule, InputTextModule, InputNumberModule, DropdownModule, ChipModule, TooltipModule
  ],
  templateUrl: './product-detail.component.html'
})
export class ProductDetailComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  protected readonly authService = inject(AuthService);

  product = signal<ProductDetail | null>(null);
  units: UnitOption[] = [];

  showAddPresentation = false;
  presentationForm = this.fb.nonNullable.group({
    unitCode: ['', Validators.required],
    conversionFactor: this.fb.nonNullable.control<number>(1, [Validators.required, Validators.min(0.000001)]),
    salePrice: this.fb.nonNullable.control<number>(0, Validators.required),
    purchasePrice: this.fb.control<number | null>(null),
    barcode: ['']
  });
  savingPresentation = signal(false);

  showPriceDialog = false;
  priceForm = this.fb.nonNullable.group({
    newPrice: this.fb.nonNullable.control<number>(0, Validators.required),
    reason: ['']
  });
  selectedPresentation: Presentation | null = null;
  savingPrice = signal(false);

  showHistoryDialog = false;
  priceHistory: PriceHistoryEntry[] = [];

  private productId!: number;

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.units().subscribe((units) => (this.units = units));
    this.reload();
  }

  reload(): void {
    this.productService.findById(this.productId).subscribe((product) => this.product.set(product));
  }

  toggleStatus(): void {
    const current = this.product();
    if (!current) return;
    const next = current.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.productService.setStatus(current.id, next).subscribe(() => this.reload());
  }

  openAddPresentation(): void {
    this.presentationForm.reset({ unitCode: '', conversionFactor: 1, salePrice: 0, purchasePrice: null, barcode: '' });
    this.showAddPresentation = true;
  }

  submitPresentation(): void {
    if (this.presentationForm.invalid) {
      this.presentationForm.markAllAsTouched();
      return;
    }
    const raw = this.presentationForm.getRawValue();
    this.savingPresentation.set(true);

    this.productService.addPresentation(this.productId, {
      unitCode: raw.unitCode,
      conversionFactor: raw.conversionFactor,
      salePrice: raw.salePrice,
      purchasePrice: raw.purchasePrice,
      barcode: raw.barcode || null
    }).subscribe({
      next: () => {
        this.savingPresentation.set(false);
        this.showAddPresentation = false;
        this.reload();
      },
      error: () => this.savingPresentation.set(false)
    });
  }

  openPriceDialog(presentation: Presentation): void {
    this.selectedPresentation = presentation;
    this.priceForm.reset({ newPrice: presentation.salePrice, reason: '' });
    this.showPriceDialog = true;
  }

  submitPrice(): void {
    if (!this.selectedPresentation || this.priceForm.invalid) {
      return;
    }
    const raw = this.priceForm.getRawValue();
    this.savingPrice.set(true);

    this.productService.updatePrice(this.selectedPresentation.id, raw.newPrice, raw.reason || undefined).subscribe({
      next: () => {
        this.savingPrice.set(false);
        this.showPriceDialog = false;
        this.reload();
      },
      error: () => this.savingPrice.set(false)
    });
  }

  openHistory(presentation: Presentation): void {
    this.productService.priceHistory(presentation.id).subscribe((history) => {
      this.priceHistory = history;
      this.showHistoryDialog = true;
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/productos');
  }

  goToInventory(): void {
    this.router.navigateByUrl(`/inventario/productos/${this.productId}`);
  }
}
