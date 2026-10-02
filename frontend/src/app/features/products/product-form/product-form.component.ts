import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextarea } from 'primeng/inputtextarea';
import { DropdownModule } from 'primeng/dropdown';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { CardModule } from 'primeng/card';

import { ProductService } from '../services/product.service';
import { SimpleCatalogEntry, UnitOption } from '../services/product.models';

/**
 * Alta de producto (Fase 3): crea el producto junto con su presentacion
 * base (factor 1 en la unidad base elegida). Las presentaciones
 * adicionales (tableta, caja...) se agregan despues desde el detalle,
 * una vez el producto existe.
 */
@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, InputTextModule, InputNumberModule,
    InputTextarea, DropdownModule, CheckboxModule, ButtonModule, MessageModule, CardModule
  ],
  templateUrl: './product-form.component.html'
})
export class ProductFormComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    baseUnitCode: ['', Validators.required],
    categoryId: this.fb.control<number | null>(null),
    laboratoryId: this.fb.control<number | null>(null),
    activeIngredientId: this.fb.control<number | null>(null),
    concentration: [''],
    pharmaceuticalForm: [''],
    brand: [''],
    description: [''],
    minStockBase: this.fb.nonNullable.control<number>(0),
    maxStockBase: this.fb.control<number | null>(null),
    requiresPrescription: [false],
    fractionable: [true],
    controlsLot: [true],
    controlsExpiration: [true],
    salePrice: this.fb.nonNullable.control<number>(0, Validators.required),
    purchasePrice: this.fb.control<number | null>(null),
    barcode: ['']
  });

  units: UnitOption[] = [];
  categories: SimpleCatalogEntry[] = [];
  laboratories: SimpleCatalogEntry[] = [];
  activeIngredients: SimpleCatalogEntry[] = [];

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.productService.units().subscribe((units) => (this.units = units));
    this.productService.categories().subscribe((categories) => (this.categories = categories));
    this.productService.laboratories().subscribe((laboratories) => (this.laboratories = laboratories));
    this.productService.activeIngredients().subscribe((ingredients) => (this.activeIngredients = ingredients));
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    this.productService.create({
      name: raw.name,
      baseUnitCode: raw.baseUnitCode,
      categoryId: raw.categoryId,
      laboratoryId: raw.laboratoryId,
      activeIngredientId: raw.activeIngredientId,
      concentration: raw.concentration || null,
      pharmaceuticalForm: raw.pharmaceuticalForm || null,
      brand: raw.brand || null,
      description: raw.description || null,
      minStockBase: raw.minStockBase,
      maxStockBase: raw.maxStockBase,
      requiresPrescription: raw.requiresPrescription,
      fractionable: raw.fractionable,
      controlsLot: raw.controlsLot,
      controlsExpiration: raw.controlsExpiration,
      basePresentation: {
        unitCode: raw.baseUnitCode,
        conversionFactor: 1,
        salePrice: raw.salePrice,
        purchasePrice: raw.purchasePrice,
        barcode: raw.barcode || null
      }
    }).subscribe({
      next: (product) => {
        this.saving.set(false);
        this.router.navigateByUrl(`/productos/${product.id}`);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo crear el producto.');
      }
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/productos');
  }
}
