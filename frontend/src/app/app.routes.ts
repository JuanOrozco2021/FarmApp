import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { LoginComponent } from './features/auth/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProductListComponent } from './features/products/product-list/product-list.component';
import { ProductFormComponent } from './features/products/product-form/product-form.component';
import { ProductDetailComponent } from './features/products/product-detail/product-detail.component';
import { InventoryAlertsComponent } from './features/inventory/alerts/inventory-alerts.component';
import { InventoryKardexComponent } from './features/inventory/kardex/inventory-kardex.component';
import { SupplierListComponent } from './features/purchasing/suppliers/supplier-list.component';
import { PurchaseListComponent } from './features/purchasing/purchase-list/purchase-list.component';
import { PurchaseFormComponent } from './features/purchasing/purchase-form/purchase-form.component';
import { PurchaseDetailComponent } from './features/purchasing/purchase-detail/purchase-detail.component';
import { PosComponent } from './features/sales/pos/pos.component';
import { SaleListComponent } from './features/sales/sale-list/sale-list.component';
import { SaleDetailComponent } from './features/sales/sale-detail/sale-detail.component';
import { CustomerListComponent } from './features/customers/customer-list/customer-list.component';
import { CustomerDetailComponent } from './features/customers/customer-detail/customer-detail.component';
import { InjectionListComponent } from './features/injections/injection-list/injection-list.component';
import { InjectionFormComponent } from './features/injections/injection-form/injection-form.component';
import { InjectionDetailComponent } from './features/injections/injection-detail/injection-detail.component';
import { AuditListComponent } from './features/audit/audit-list/audit-list.component';
import { SettingsComponent } from './features/settings/settings.component';
import { UserListComponent } from './features/users/user-list/user-list.component';

// Cada feature futura (inventario, compras, pos, clientes, proveedores,
// inyectologia, reportes) se agrega aqui como ruta hija, sin tocar el shell.
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'productos', component: ProductListComponent },
      { path: 'productos/nuevo', component: ProductFormComponent },
      { path: 'productos/:id', component: ProductDetailComponent },
      { path: 'inventario/alertas', component: InventoryAlertsComponent },
      { path: 'inventario/productos/:id', component: InventoryKardexComponent },
      { path: 'proveedores', component: SupplierListComponent },
      { path: 'compras', component: PurchaseListComponent },
      { path: 'compras/nueva', component: PurchaseFormComponent },
      { path: 'compras/:id/editar', component: PurchaseFormComponent },
      { path: 'compras/:id', component: PurchaseDetailComponent },
      { path: 'pos', component: PosComponent },
      { path: 'ventas', component: SaleListComponent },
      { path: 'ventas/:id', component: SaleDetailComponent },
      { path: 'clientes', component: CustomerListComponent },
      { path: 'clientes/:id', component: CustomerDetailComponent },
      { path: 'inyectologia', component: InjectionListComponent },
      { path: 'inyectologia/nueva', component: InjectionFormComponent },
      { path: 'inyectologia/:id', component: InjectionDetailComponent },
      { path: 'auditoria', component: AuditListComponent },
      { path: 'configuracion', component: SettingsComponent },
      { path: 'usuarios', component: UserListComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
