/**
 * STILLFEED — Supabase Auth "Send Email Hook" (Resend Integration)
 *
 * This server-side Edge Function intercepts Supabase Auth email triggers
 * (confirmation signup OTP, password recovery, magic link) and dispatches
 * branded emails via the Resend REST API (https://api.resend.com/emails).
 *
 * Security Guarantee:
 * - NO Resend API keys or secrets are EVER bundled into the Android APK.
 * - The RESEND_API_KEY is stored strictly in Supabase Edge Function Secrets.
 *
 * To deploy and enable:
 * 1. supabase secrets set RESEND_API_KEY="re_..." RESEND_FROM="STILLFEED <noreply@yourdomain.com>"
 * 2. supabase functions deploy resend-email-hook --no-verify-jwt
 * 3. In Supabase Dashboard -> Auth -> Hooks: Enable "Send Email Hook" pointing to resend-email-hook
 */

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

interface WebhookPayload {
  user: {
    id: string;
    email: string;
    user_metadata?: {
      full_name?: string;
    };
  };
  email_data: {
    token: string;
    token_hash: string;
    redirect_to: string;
    email_action_type: "signup" | "recovery" | "magiclink" | "invite" | "email_change";
    site_url: string;
  };
}

serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response("Method Not Allowed", { status: 405 });
  }

  try {
    const payload: WebhookPayload = await req.json();
    const { user, email_data } = payload;
    const resendApiKey = Deno.env.get("RESEND_API_KEY");
    const resendFrom = Deno.env.get("RESEND_FROM") || "STILLFEED <onboarding@resend.dev>";

    if (!resendApiKey) {
      console.error("RESEND_API_KEY environment variable is not configured.");
      return new Response(JSON.stringify({ error: "Missing Resend API key" }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }

    const { email_action_type, token } = email_data;
    let subject = "STILLFEED Verification Code";
    let htmlContent = "";

    if (email_action_type === "signup") {
      subject = "Your STILLFEED Verification Code";
      htmlContent = `
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #F5F0E7; padding: 40px 20px; color: #1F2421;">
          <div style="max-width: 480px; margin: 0 auto; background-color: #FBF8F2; border-radius: 16px; border: 1px solid #D9D4CA; padding: 32px;">
            <h1 style="font-size: 24px; font-weight: 600; margin-bottom: 8px; color: #1F2421;">Check your email.</h1>
            <p style="font-size: 15px; color: #6D746E; margin-bottom: 24px;">Enter the following six-digit code to finish creating your account:</p>
            <div style="background-color: #F5F0E7; border-radius: 12px; padding: 18px; text-align: center; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #1F2421; margin-bottom: 24px;">
              ${token}
            </div>
            <p style="font-size: 13px; color: #6D746E; line-height: 18px; margin: 0;">This code expires in 10 minutes. Keep it private. Never share it with anyone.</p>
          </div>
        </div>
      `;
    } else if (email_action_type === "recovery") {
      subject = "Reset your STILLFEED password";
      htmlContent = `
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #F5F0E7; padding: 40px 20px; color: #1F2421;">
          <div style="max-width: 480px; margin: 0 auto; background-color: #FBF8F2; border-radius: 16px; border: 1px solid #D9D4CA; padding: 32px;">
            <h1 style="font-size: 24px; font-weight: 600; margin-bottom: 8px; color: #1F2421;">Reset your password.</h1>
            <p style="font-size: 15px; color: #6D746E; margin-bottom: 24px;">We received a request to reset your password. Use the code below:</p>
            <div style="background-color: #F5F0E7; border-radius: 12px; padding: 18px; text-align: center; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #1F2421; margin-bottom: 24px;">
              ${token}
            </div>
            <p style="font-size: 13px; color: #6D746E; line-height: 18px; margin: 0;">If you did not request this, you can safely ignore this email.</p>
          </div>
        </div>
      `;
    } else {
      subject = "STILLFEED Authentication Code";
      htmlContent = `<p>Your code is: <strong>${token}</strong></p>`;
    }

    const resendResponse = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${resendApiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: resendFrom,
        to: [user.email],
        subject: subject,
        html: htmlContent,
      }),
    });

    if (!resendResponse.ok) {
      const errText = await resendResponse.text();
      console.error("Resend API error:", errText);
      return new Response(JSON.stringify({ error: "Failed to dispatch email", details: errText }), {
        status: 502,
        headers: { "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ success: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (err: unknown) {
    const error = err as Error;
    console.error("Hook error:", error.message);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
