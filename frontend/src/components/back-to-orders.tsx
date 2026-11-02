import '@/app/back-to-orders.css';

/** Consistent return navigation for customer and staff order details. */
export function BackToOrders({href}:{href:string}){
 return <a className="back-to-orders" href={href}><svg aria-hidden="true" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m10 5-7 7 7 7M3 12h18"/></svg><span>Volver a pedidos</span></a>;
}
