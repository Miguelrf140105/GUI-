export interface Proveedor {
  id: number;
  name: string;
  document: string;
  contact?: string;
  email?: string;
  phone?: string;
  address?: string;
  stateId: number;
}
export interface BusquedaProveedor {
  found: boolean;
  institution?: Proveedor;
}
