export interface PaymentLinkRequest {
  businessId: string;
  amount: number;
  description: string;
  customerName?: string;
  customerPhone?: string;
}

export interface PaymentLinkResponse {
  paymentLinkId: string;
  paymentUrl: string;
  gatewayReferenceId: string;
  amount: number;
  status: 'PENDING' | 'PAID' | 'EXPIRED';
}

export interface PaymentGateway {
  createPaymentLink(request: PaymentLinkRequest): Promise<PaymentLinkResponse>;
  verifyPaymentSignature(gatewayReferenceId: string, signature: string): Promise<boolean>;
}

export class MockPaymentGateway implements PaymentGateway {
  async createPaymentLink(request: PaymentLinkRequest): Promise<PaymentLinkResponse> {
    const refId = `pay_link_${Date.now()}`;
    return {
      paymentLinkId: `pl_${Math.random().toString(36).substring(7)}`,
      paymentUrl: `https://pay.buztrack.app/${refId}`,
      gatewayReferenceId: refId,
      amount: request.amount,
      status: 'PENDING',
    };
  }

  async verifyPaymentSignature(_gatewayReferenceId: string, _signature: string): Promise<boolean> {
    return true;
  }
}
